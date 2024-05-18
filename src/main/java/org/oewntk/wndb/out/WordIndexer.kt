/*
 * Copyright (c) 2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.Category
import org.oewntk.model.Sense
import org.oewntk.model.SenseGroupings.sensesByLCLemmaAndPos
import org.oewntk.model.Synset
import org.oewntk.wndb.out.Formatter.joinToStringWithCount
import org.oewntk.wndb.out.Formatter.offsetFormat
import java.io.PrintStream
import java.util.*

/**
 * This class produces the 'index.{noun|verb|adj|adv}' files
 *
 * @property offsets offsets indexed by synset id key
 * @property flags   flags
 * @author Bernard Bou
 */
class WordIndexer(
    private val offsets: Map<String, Long>,
    private val flags: Int,
) {

    /**
     * This represents what is needed for a line in index.(noun|verb|adj|adv)
     */
    private class IndexEntry(val category: Category) {

        var taggedSensesCount: Int = 0
            private set

        val synsetIds: MutableSet<String> = LinkedHashSet()

        val relationPointers: MutableSet<String> = TreeSet()

        fun incTaggedCount() {
            taggedSensesCount += 1
        }
    }

    /**
     * Make index
     *
     * @param ps          print stream
     * @param senses      senses
     * @param synsetsById synsets mapped by id
     * @param posFilter   part-of-speech filter for lexes
     * @return number of indexes
     */
    fun make(ps: PrintStream, senses: Collection<Sense>, synsetsById: Map<String, Synset>, posFilter: Char): Long {
        val incompats: MutableMap<String, Int> = HashMap()

        // file header
        ps.print(Formatter.OEWN_HEADER)

        // collect entries
        val pointerCompat = (flags and Flags.POINTER_COMPAT) != 0
        val indexEntries: MutableMap<String, IndexEntry> = TreeMap()

        val groupedSenses = sensesByLCLemmaAndPos(senses)
        groupedSenses.entries
            .asSequence()
            .filter { it.key.category == posFilter }
            .sortedBy { it.key.lcLemma.replace(' ', '_') }
            .forEach { (key, senses) ->
                val kSenses = senses
                    .asSequence()
                    .sortedWith(SenseComparator.WNDB_SENSE_ORDER)
                    .toList()

                val lcLemma = key.lcLemma
                val pos = key.category
                val ik = lcLemma.replace(' ', '_')
                val eik = Formatter.escape(ik)

                // init data mapped by lower-cased lemma
                val indexEntry = indexEntries.computeIfAbsent(eik) { IndexEntry(pos) }

                // synset ids
                collectSynsetIds(kSenses, indexEntry.synsetIds)

                // tag counts
                collectTagCounts(kSenses, indexEntry)

                // synset relations
                collectSynsetRelations(kSenses, synsetsById, pos, indexEntry.relationPointers, pointerCompat, incompats)

                // sense relations
                collectSenseRelations(kSenses, pos, indexEntry.relationPointers, pointerCompat, incompats)

                // print
                printIndexEntry(eik, indexEntry, ps)
            }

        // log
        reportIncompats(incompats)
        return indexEntries.size.toLong()
    }

    /**
     * Collect synset ids
     *
     * @param senses    senses
     * @param synsetIds set to collect to
     */
    private fun collectSynsetIds(senses: List<Sense>, synsetIds: MutableSet<String>) {
        senses.forEach {
            synsetIds.add(it.synsetId)
        }
    }

    /**
     * Collect tag counts
     *
     * @param senses     senses
     * @param indexEntry to collect to
     */
    private fun collectTagCounts(senses: List<Sense>, indexEntry: IndexEntry) {
        senses
            .filter { it.tagCount != null && it.tagCount!!.count > 0 }
            .forEach { _ ->
                indexEntry.incTaggedCount()
            }
    }

    /**
     * Collect synset relations
     *
     * @param senses        senses
     * @param synsetsById   synsets by id
     * @param category           pos
     * @param pointers      set of pointers to collect to
     * @param pointerCompat pointer compatibility flag
     * @param incompats     incompatibility log
     */
    private fun collectSynsetRelations(senses: List<Sense>, synsetsById: Map<String, Synset>, category: Category, pointers: MutableSet<String>, pointerCompat: Boolean, incompats: MutableMap<String, Int>) {
        senses.forEach {
            val synset = synsetsById[it.synsetId]!!
            collectSynsetRelations(synset, category, pointers, pointerCompat, incompats)
        }
    }

    /**
     * Collect sense relations
     *
     * @param synset        synset
     * @param category           pos
     * @param pointers      set of pointers to collect to
     * @param pointerCompat pointer compatibility flag
     * @param incompats     incompatibility log
     */
    private fun collectSynsetRelations(synset: Synset, category: Category, pointers: MutableSet<String>, pointerCompat: Boolean, incompats: MutableMap<String, Int>) {
        if (!synset.relations.isNullOrEmpty()) {
            synset.relations!!.keys.forEach { relation ->
                try {
                    val pointer: String = Coder.codeRelation(relation, category, pointerCompat)
                    pointers.add(pointer)

                } catch (e: CompatException) {
                    val cause = e.cause!!.message!!
                    val count = incompats.computeIfAbsent(cause) { 0 } + 1
                    incompats[cause] = count
                } catch (e: IllegalArgumentException) {
                    if (LOG) {
                        Tracing.psErr.println("[W] Discarded relation '$relation'")
                    }
                    throw e
                }
            }
        }
    }

    /**
     * Collect sense relations
     *
     * @param senses        senses
     * @param category           pos
     * @param pointers      set of pointers to collect to
     * @param pointerCompat pointer compatibility flag
     * @param incompats     incompatibility log
     */
    private fun collectSenseRelations(senses: List<Sense>, category: Category, pointers: MutableSet<String>, pointerCompat: Boolean, incompats: MutableMap<String, Int>) {
        senses
            .filter { !it.relations.isNullOrEmpty() }
            .flatMap { it.relations!!.keys }
            .forEach { relation ->
                try {
                    val pointer: String = Coder.codeRelation(relation, category, pointerCompat)
                    pointers.add(pointer)
                } catch (e: CompatException) {
                    val cause = e.cause!!.message!!
                    val count = incompats.computeIfAbsent(cause) { 0 } + 1
                    incompats[cause] = count
                } catch (e: IllegalArgumentException) {
                    if (LOG) {
                        Tracing.psErr.println("[W] Discarded relation '$relation'")
                    }
                }
            }
    }

    /**
     * Print index entry
     *
     * @param key        index key
     * @param indexEntry index data
     * @param ps         print stream
     * ```
     * lemma
     * pos
     * synset_cnt
     * ptrs=p_cnt  [ptr_symbol...]
     * ofs=sense_cnt
     * tag sense cnt (number of senses that are tagged)
     * synset_offset  [synset_offset...]
     * ```
     */
    private fun printIndexEntry(key: String, indexEntry: IndexEntry, ps: PrintStream) {
        val nSenses = indexEntry.synsetIds.size
        val ptrs = indexEntry.relationPointers.joinToStringWithCount(countFormat = Int::toString)
        val ofs = indexEntry.synsetIds.joinToString(separator = " ") { offsetFormat(offsets[it]!!) }
        val line = "$key ${indexEntry.category} $nSenses $ptrs $nSenses ${indexEntry.taggedSensesCount} $ofs  "
        ps.println(line)
    }

    /**
     * Report incompatibilities to log
     *
     * @param incompats incompatibilities
     */
    private fun reportIncompats(incompats: Map<String, Int>) {
        if (incompats.isNotEmpty()) {
            incompats.forEach { (key, value) ->
                Tracing.psErr.println("[W] Incompatibilities '$key': $value")
            }
        }
    }

    companion object {

        private const val LOG = false
    }
}
