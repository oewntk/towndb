/*
 * Copyright (c) 2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.Lex
import org.oewntk.model.Sense
import org.oewntk.model.Synset
import org.oewntk.wndb.out.Coder.codeFrameId
import org.oewntk.wndb.out.Coder.codeLexFile
import org.oewntk.wndb.out.Data.AdjMember
import org.oewntk.wndb.out.Formatter.intFormat2
import org.oewntk.wndb.out.Formatter.intFormat3
import org.oewntk.wndb.out.Formatter.intFormatHex2X
import org.oewntk.wndb.out.Formatter.joinToStringWithCount
import org.oewntk.wndb.out.Formatter.offsetFormat
import java.util.*

/**
 * This abstract class iterates over the synsets to produce a line of data. The real classes implement some functions differently.
 *
 * @author Bernard Bou
 */
abstract class SynsetProcessor
protected constructor(
    /**
     * Lexes mapped by lemma
     */
    protected val lexesByLemma: Map<String, Collection<Lex>>,

    /**
     * Synsets mapped by id
     */
    protected val synsetsById: Map<String, Synset>,

    /**
     * Senses mapped by id
     */
    protected val sensesById: Map<String, Sense>,

    /**
     * Function that, when applied to a synsetId, yields the synset offset in the data files. May be dummy constant function.
     */
    protected val offsetFunction: (String) -> Long,

    /**
     * Flags
     */
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
    internal class RelationData(private val isSenseRelation: Boolean, val relType: String, val target: String) : Comparable<RelationData> {

        // identity
        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other == null || javaClass != other.javaClass) {
                return false
            }
            val that = other as RelationData
            return isSenseRelation == that.isSenseRelation && relType == that.relType && target == that.target
        }

        override fun hashCode(): Int {
            return Objects.hash(isSenseRelation, relType, target)
        }

        // order
        override fun compareTo(other: RelationData): Int {
            var c = java.lang.Boolean.compare(this.isSenseRelation, other.isSenseRelation)
            if (c != 0) {
                return c
            }
            c = relType.compareTo(other.relType)
            if (c != 0) {
                return c
            }
            return relType.compareTo(other.relType)
        }

        // string
        override fun toString(): String {
            return (if (this.isSenseRelation) "SenseRelation" else "SynsetRelation") + " relType=" + this.relType + " target=" + this.target
        }
    }

    /**
     * Build member (escaped lemma, lexid, adj position)
     *
     * @param sense sense
     * @return member
     */
    private fun buildMember(sense: Sense): Data.Member {
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
        return if (adjPosition.isNullOrEmpty()) Data.Member(escaped, lexid, lexIdCompat) else AdjMember(escaped, lexid, adjPosition, lexIdCompat)
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
        // init
        val relations: MutableList<Data.Relation> = ArrayList()
        val frames = Data.Frames()

        // attribute data
        val type = synset.type

        // build members ordered set
        val members: MutableList<Data.Member> = ArrayList()
        for (lemma in synset.members) {
            val sense = checkNotNull(synset.findSenseOf(lemma, lexesByLemma)) { "find sense of '$lemma' in synset $synset" }
            val member = buildMember(sense)
            members.add(member)
        }

        // definition and examples
        // allow multiple definitions and join them
        val definitions = synset.definitions
        val examples = synset.examples

        // lexfile num
        val lexfileNum = buildLexfileNum(synset)

        // synset relations
        if (!synset.relations.isNullOrEmpty()) {
            val relationDataSet: MutableSet<RelationData> = LinkedHashSet()
            for ((relationType, values) in synset.relations!!) {
                for (targetSynsetId in values) {
                    val relation = RelationData(false, relationType, targetSynsetId)
                    val wasThere = !relationDataSet.add(relation)
                    if (wasThere && LOG_DUPLICATE_RELATION && log()) {
                        Tracing.psErr.printf("[W] Synset %s has duplicate %s%n", synset.synsetId, relation)
                    }
                }
            }
            val pointerCompat = (flags and Flags.POINTER_COMPAT) != 0
            for (relationData in relationDataSet) {
                val targetSynset = synsetsById[relationData.target]

                val targetOffset = offsetFunction.invoke(relationData.target)
                val targetType = targetSynset!!.type
                var relation: Data.Relation
                try {
                    relation = Data.Relation(relationData.relType, type, targetType, targetOffset, 0, 0, pointerCompat)
                } catch (e: CompatException) {
                    val cause = e.cause!!.message!!
                    val count = incompats.computeIfAbsent(cause) { 0 } + 1
                    incompats[cause] = count
                    continue
                } catch (e: IllegalArgumentException) {
                    if (LOG_DISCARDED && log()) {
                        Tracing.psErr.printf("[W] Discarded relation '%s' synset=%s offset=%d%n", relationData.relType, synset.synsetId, offset)
                    }
                    throw e
                }
                relations.add(relation)
            }
        }

        // senses that have this synset as target in "synset" field
        val senses = synset.findSenses(lexesByLemma)
        assert(senses.isNotEmpty())

        // iterate senses
        for (sense in senses) {
            val verbFrameCompat = (flags and Flags.VERBFRAME_COMPAT) != 0

            // verb frames attribute
            val verbFrameIds = sense.verbFrames
            if (!verbFrameIds.isNullOrEmpty()) {
                for (verbframeId in verbFrameIds) {
                    try {
                        val frame = Data.Frame(codeFrameId(verbframeId, verbFrameCompat), sense.findSynsetIndex(synsetsById) + 1)
                        frames.add(frame)
                    } catch (e: CompatException) {
                        val cause = e.cause!!.message!!
                        val count = incompats.computeIfAbsent(cause) { 0 } + 1
                        incompats[cause] = count
                    }
                }
            }

            // sense relations
            if (!sense.relations.isNullOrEmpty()) {
                val lemma = sense.lemma

                val senseRelationDataSet: MutableSet<RelationData> = LinkedHashSet()
                for ((relationType, values) in sense.relations!!) {
                    for (targetSenseId in values) {
                        val relation = RelationData(true, relationType, targetSenseId)
                        val wasThere = !senseRelationDataSet.add(relation)
                        if (wasThere && LOG_DUPLICATE_RELATION && log()) {
                            Tracing.psErr.printf("[W] Sense %s has duplicate %s%n", sense.senseKey, relation)
                        }
                    }
                }

                for (relationData in senseRelationDataSet) {
                    val targetSense = sensesById[relationData.target]!!
                    val targetSynsetId = targetSense.synsetId
                    val targetSynset = synsetsById[targetSynsetId]!!

                    val memberNum = synset.findIndexOfMember(lemma) + 1

                    var relation: Data.Relation
                    try {
                        relation = buildSenseRelation(relationData.relType, type, memberNum, targetSense, targetSynset, targetSynsetId)
                    } catch (e: CompatException) {
                        val cause = e.cause!!.message!!
                        val count = incompats.computeIfAbsent(cause) { 0 } + 1
                        incompats[cause] = count
                        continue
                    } catch (e: IllegalArgumentException) {
                        if (LOG_DISCARDED && log()) {
                            // String cause = e.getClass().getName() + ' ' + e.getMessage();
                            Tracing.psErr.printf("[W] Discarded relation '%s' synset=%s offset=%d%n", relationData.relType, synset.synsetId, offset)
                        }
                        // throw e;
                        continue
                    }
                    relations.add(relation)
                }
            }
        }

        // assemble
        val lexIdCompat = (flags and Flags.LEXID_COMPAT) != 0
        val membersData = members.joinToStringWithCount(separator = " ", countSeparator = " ", countFormat = ::intFormatHex2X) { it.toWndbString(lexIdCompat) }
        val relatedData = relations.joinToStringWithCount(separator = " ", countSeparator = " ", countFormat = ::intFormat3) { it.toWndbString() }

        var verbframesData = frames.toWndbString(type, members.size)
        if (verbframesData.isNotEmpty()) {
            verbframesData = " $verbframesData"
        }
        val definitionsData = definitions.joinToString("; ")
        val examplesData = if (examples.isNullOrEmpty()) "" else "; " + examples.joinToString(" ")
        return "${offsetFormat(offset)} ${intFormat2(lexfileNum)} $type $membersData $relatedData$verbframesData | $definitionsData$examplesData  \n"
    }

    /**
     * Build relation
     *
     * @param type            relation type
     * @param pos             part of speech
     * @param sourceMemberNum 1-based index of source member in source synset
     * @param targetSense     target sense
     * @param targetSynset    target synset
     * @param targetSynsetId  target synsetid
     * @return relation
     * @throws CompatException when relation is not legacy compatible
     */
    @Throws(CompatException::class)
    protected open fun buildSenseRelation(type: String, pos: Char, sourceMemberNum: Int, targetSense: Sense, targetSynset: Synset, targetSynsetId: String): Data.Relation {
        // target lemma
        val targetLemma = targetSense.lemma

        // which
        val targetMemberNum = targetSynset.findIndexOfMember(targetLemma) + 1
        val targetType = targetSynset.type
        val targetOffset = offsetFunction.invoke(targetSynsetId)
        val pointerCompat = (flags and Flags.POINTER_COMPAT) != 0
        return Data.Relation(type, pos, targetType, targetOffset, sourceMemberNum, targetMemberNum, pointerCompat)
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
            for ((key, value) in incompats) {
                Tracing.psErr.printf("[W] Incompatibilities '%s': %d%n", key, value)
            }
            incompats.clear()
        }
    }

    companion object {

        private const val LOG_DISCARDED = false

        private const val LOG_DUPLICATE_RELATION = false
    }
}
