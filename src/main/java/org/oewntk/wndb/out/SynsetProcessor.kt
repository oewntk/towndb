/*
 * Copyright (c) 2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.*
import org.oewntk.wndb.out.Coder.codeFrameId
import org.oewntk.wndb.out.Coder.codeLexFile
import org.oewntk.wndb.out.Data.AdjMember
import org.oewntk.wndb.out.Data.Member
import org.oewntk.wndb.out.Data.Relation
import org.oewntk.wndb.out.Data.VerbFrame
import org.oewntk.wndb.out.Data.VerbFrame.Companion.toWndbString
import org.oewntk.wndb.out.Formatter.intFormat2
import org.oewntk.wndb.out.Formatter.intFormat3
import org.oewntk.wndb.out.Formatter.intFormatHex2x
import org.oewntk.wndb.out.Formatter.joinToStringWithCount
import org.oewntk.wndb.out.Formatter.offsetFormat
import java.util.*

/**
 * This abstract class iterates over the synsets to produce a line of data. The real classes implement some functions differently.

 * @property lexesByLemma lexes mapped by lemma
 * @property synsetsById synsets mapped by id
 * @property sensesById senses mapped by id
 * @property offsetFunction function that, when applied to a synsetId, yields the synset offset in the data files. May be dummy constant function.
 * @property flags flags
 *
 * @author Bernard Bou
 */
abstract class SynsetProcessor protected constructor(

    protected val lexesByLemma: Map<String, Collection<Lex>>,
    protected val synsetsById: Map<String, Synset>,
    protected val sensesById: Map<String, Sense>,
    protected val offsetFunction: (String) -> Long,
    protected val flags: Int,

    ) {

    /**
     * Report incompatibility counts (indexed by cause)
     */
    private val incompats: MutableMap<String, Int> = HashMap()

    /**
     * Log error flag (avoid duplicate messages)
     *
     * @return whether to log errors
     */
    protected abstract fun log(): Boolean

    /**
     * Intermediate class to detect duplicates
     */
    internal class RelationData(private val isSenseRelation: Boolean, val relation: String, val target: String) : Comparable<RelationData> {

        // identity
        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other == null || javaClass != other.javaClass) {
                return false
            }
            val that = other as RelationData
            return isSenseRelation == that.isSenseRelation && relation == that.relation && target == that.target
        }

        override fun hashCode(): Int {
            return Objects.hash(isSenseRelation, relation, target)
        }

        // order
        override fun compareTo(other: RelationData): Int {
            var c = java.lang.Boolean.compare(this.isSenseRelation, other.isSenseRelation)
            if (c != 0) {
                return c
            }
            c = relation.compareTo(other.relation)
            if (c != 0) {
                return c
            }
            return relation.compareTo(other.relation)
        }

        // string
        override fun toString(): String {
            return (if (this.isSenseRelation) "SenseRelation" else "SynsetRelation") + " relType=" + this.relation + " target=" + this.target
        }
    }

    /**
     * Build member (escaped lemma, lexid, adj position)
     *
     * @param sense sense
     * @return member
     */
    private fun buildMember(sense: Sense): Member {
        // lexid
        val lexid = sense.findLexid()

        // adjPosition
        val adjPosition = sense.adjPosition

        // lex
        val lex = sense.lex

        // lemma
        val lemma = lex.lemma

        val escaped = Formatter.escape(lemma)
        val lexIdCompat = (flags and Flags.LEXID_COMPAT) != 0
        return if (adjPosition.isNullOrEmpty()) Member(escaped, lexid, lexIdCompat) else AdjMember(escaped, lexid, adjPosition, lexIdCompat)
    }

    /**
     * Get data and yield line
     *
     * @param synset synset
     * @param offset allocated offset for the synset
     * @return line
     * ```
     * offset
     * lexfile num
     * pos
     * members
     * relations
     * frames
     * definition
     * example
     * ```
     */
    fun getData(synset: Synset, offset: Long): String {

        // attribute data
        val type = synset.type

        // build members ordered set
        val members: List<Member> = synset.members
            .map { member -> synset.findSenseOf(member, { lexesByLemma[it] }, { sensesById[it]!! }) }
            .map { buildMember(it) }
            .toList()

        // definition and examples
        // allow multiple definitions and join them
        val definitions = synset.definitions
        val examples = synset.examples

        // lexfile num
        val lexfileNum = buildLexfileNum(synset)

        // synset relations
        val synsetRelations = getSynsetRelationData(synset, offset)

        // senses that have this synset as target in "synset" field
        val senses = synset.findSenses(
            { lemma -> lexesByLemma[lemma]!! },
            { senseKey -> sensesById[senseKey]!! },
        )
        assert(senses.isNotEmpty())

        // sense relations
        val senseRelations = getSenseRelationData(synset, senses, offset)

        // verb frames
        val verbFrames = getVerbFrames(senses)

        // assemble
        val lexIdCompat = (flags and Flags.LEXID_COMPAT) != 0
        val membersData = members.joinToStringWithCount(separator = " ", countSeparator = " ", countFormat = ::intFormatHex2x) { it.toWndbString(lexIdCompat) }
        val allRelations = synsetRelations + senseRelations
        val relatedData = allRelations.joinToStringWithCount(separator = " ", countSeparator = " ", countFormat = ::intFormat3) { it.toWndbString() }
        var verbframesData = verbFrames.toWndbString(type, members.size)
        if (verbframesData.isNotEmpty()) {
            verbframesData = " $verbframesData"
        }
        val definitionsData = definitions.joinToString("; ")
        val examplesData = if (examples.isNullOrEmpty()) "" else "; " + examples.joinToString(separator = " ") { example -> "\"$example\"" }
        return "${offsetFormat(offset)} ${intFormat2(lexfileNum)} $type $membersData $relatedData$verbframesData | $definitionsData$examplesData  \n"
    }

    private fun getSynsetRelationData(synset: Synset, offset: Long): List<Relation> {

        return if (synset.relations.isNullOrEmpty()) emptyList() else {
            val pointerCompat = (flags and Flags.POINTER_COMPAT) != 0
            synset.relations!!
                .asSequence()
                .flatMap { (relationType, targetSynsets) ->
                    targetSynsets
                        .asSequence()
                        .map { targetSenseId -> RelationData(false, relationType, targetSenseId) }
                }
                .toSetWarnDuplicates { duplicate ->
                    if (LOG_DUPLICATE_RELATION && log()) {
                        Tracing.psErr.println("[W] Synset ${synset.synsetId} has duplicate ${duplicate.relation}")
                    }
                }.mapNotNull { relationData ->
                    val targetSynset = synsetsById[relationData.target]!!
                    val targetOffset = offsetFunction.invoke(relationData.target)
                    val targetType = targetSynset.type
                    try {
                        Relation(relationData.relation, synset.type, targetType, targetOffset, 0, 0, pointerCompat)

                    } catch (e: CompatException) {
                        val cause = e.cause!!.message!!
                        val count = incompats.computeIfAbsent(cause) { 0 } + 1
                        incompats[cause] = count
                        null
                    } catch (e: IllegalArgumentException) {
                        if (LOG_DISCARDED && log()) {
                            Tracing.psErr.println("[W] Discarded relation '${relationData.relation}' synset=${synset.synsetId} offset=$offset")
                        }
                        throw e
                    }
                }
                .toList()
        }
    }

    private fun getSenseRelationData(synset: Synset, senses: List<Sense>, offset: Long): List<Relation> {
        return senses
            .asSequence()
            .filter { sense -> !sense.relations.isNullOrEmpty() }
            .flatMap { sense ->
                sense.relations!!
                    .asSequence()
                    .map { entry -> sense to entry }
            }
            .flatMap { (sense, relationEntry) ->
                val relation = relationEntry.key
                val targetSynsets: MutableSet<SenseKey> = relationEntry.value
                targetSynsets
                    .asSequence()
                    .map { targetSenseId: SenseKey -> sense to RelationData(true, relation, targetSenseId) }
            }
            .toSetWarnDuplicates { (sense: Sense, duplicate: RelationData) ->
                if (LOG_DUPLICATE_RELATION && log()) {
                    Tracing.psErr.println("[W] Sense ${sense.senseKey} has duplicate ${duplicate.relation}")
                }
            }
            .mapNotNull { (sense: Sense, relationData: RelationData) ->
                val targetSense = sensesById[relationData.target]!!
                val targetSynsetId = targetSense.synsetId
                val targetSynset = synsetsById[targetSynsetId]!!

                val memberNum = synset.findIndexOfMember(sense.lemma) + 1

                try {
                    buildSenseRelation(relationData.relation, synset.type, memberNum, targetSense, targetSynset, targetSynsetId)

                } catch (e: CompatException) {
                    val cause = e.cause!!.message!!
                    val count = incompats.computeIfAbsent(cause) { 0 } + 1
                    incompats[cause] = count
                    null
                } catch (e: IllegalArgumentException) {
                    if (LOG_DISCARDED && log()) {
                        Tracing.psErr.println("[W] Discarded relation '${relationData.relation}' synset=${synset.synsetId} offset=$offset")
                    }
                    null
                    // throw e;
                }
            }
            .toList()
    }

    private fun getVerbFrames(senses: List<Sense>): Map<Int, List<VerbFrame>> {

        val verbFrameCompat = (flags and Flags.VERBFRAME_COMPAT) != 0
        return senses
            .asSequence()
            .filter { !it.verbFrames.isNullOrEmpty() }
            .flatMap { sense ->
                sense.verbFrames!!
                    .asSequence()
                    .map { verbFrameId -> sense to verbFrameId }
            }
            .map { (sense, verbframeId) ->
                try {
                    val code = codeFrameId(verbframeId, verbFrameCompat)
                    VerbFrame(code, sense.findSynsetIndex(synsetsById) + 1)
                } catch (e: CompatException) {
                    val cause = e.cause!!.message!!
                    val count = incompats.computeIfAbsent(cause) { 0 } + 1
                    incompats[cause] = count
                    null
                }
            }
            .filterNotNull()
            .groupBy { it.frameNum }
    }

    /**
     * Build relation
     *
     * @param type            relation type
     * @param category             part of speech
     * @param sourceMemberNum 1-based index of source member in source synset
     * @param targetSense     target sense
     * @param targetSynset    target synset
     * @param targetSynsetId  target synsetid
     * @return relation
     * @throws CompatException when relation is not legacy compatible
     */
    @Throws(CompatException::class)
    protected open fun buildSenseRelation(type: String, category: Category, sourceMemberNum: Int, targetSense: Sense, targetSynset: Synset, targetSynsetId: String): Relation {
        // target lemma
        val targetLemma = targetSense.lemma

        // which
        val targetMemberNum = targetSynset.findIndexOfMember(targetLemma) + 1
        val targetType = targetSynset.type
        val targetOffset = offsetFunction.invoke(targetSynsetId)
        val pointerCompat = (flags and Flags.POINTER_COMPAT) != 0
        return Relation(type, category, targetType, targetOffset, sourceMemberNum, targetMemberNum, pointerCompat)
    }

    /**
     * Build lexfile num (result does not matter in terms of output format length)
     *
     * @param synset synset
     * @return lexfile num
     */
    protected open fun buildLexfileNum(synset: Synset): Int {
        val lexfile = synset.lexfile
        try {
            return codeLexFile(lexfile!!)
        } catch (e: Exception) {
            throw IllegalArgumentException("Lexfile '$lexfile' in ${synset.synsetId}")
        }
    }

    /**
     * Report
     */
    fun report() {
        if (incompats.isNotEmpty()) {
            incompats.forEach { (key, value) ->
                Tracing.psErr.println("[W] Incompatibilities '$key': $value")
            }
            incompats.clear()
        }
    }

    companion object {

        private const val LOG_DISCARDED = false

        private const val LOG_DUPLICATE_RELATION = false
    }
}
