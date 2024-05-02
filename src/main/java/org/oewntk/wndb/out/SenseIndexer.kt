/*
 * Copyright (c) 2024. Bernard Bou.
 */

package org.oewntk.wndb.out

import org.oewntk.model.Sense
import org.oewntk.model.SenseGroupings.KeyLCLemmaAndPos.Companion.of
import org.oewntk.model.SenseGroupings.sensesByLCLemmaAndPos
import org.oewntk.wndb.out.Formatter.offsetFormat
import java.io.PrintStream

/**
 * This class produces the 'index.sense' file
 *
 * @param flags   flags
 * @param offsets synset offsets map indexed by synsetid key
 */
class SenseIndexer(
    private val flags: Int,
    private val offsets: Map<String, Long>,
) {

    /**
     * Make senses
     *
     * @param ps     print stream
     * @param senses senses
     */
    fun make(ps: PrintStream, senses: Collection<Sense>) {

        val groupedSenses = sensesByLCLemmaAndPos(senses)

        // collect
        senses
            .asSequence()
            .sortedBy(Sense::senseKey)
            .forEach {

                // sensekey
                val sensekey = it.senseKey

                // offset
                val synsetId = it.synsetId
                val offset = offsets[synsetId]!!

                // sense num
                val senseNum: Int
                if ((flags and Flags.NO_REINDEX) == 0) {
                    val k = of(it)
                    // this will yield diverging sensenums to senses varying only in lemma, not synsetid target, e.g. a%1:10:00:: and a%1:10:01::
                    // var kSenses = groupedSenses.get(k).sortedWith(SenseGroupings.BY_DECREASING_TAGCOUNT).toList()
                    // senseNum = kSenses.indexOf(sense) + 1;
                    // the following will yield the same sensenum to senses varying only in lemma, not synsetid target, e.g. a%1:10:00:: and a%1:10:01::
                    val kTargetSynsetIds = groupedSenses[k]!!
                        .sortedWith(SenseComparator.WNDB_SENSE_ORDER)
                        .map { it2 -> it2.synsetId }
                        .distinct()
                        .toList()
                    senseNum = kTargetSynsetIds.indexOf(it.synsetId) + 1
                } else {
                    senseNum = it.lexIndex + 1
                }

                // tag count
                val tagCount = it.intTagCount

                // print
                printSenseEntry(sensekey, offset, senseNum, tagCount, ps)
            }

        // log
        Tracing.psInfo.printf("Senses: %d%n", senses.size)
    }

    /**
     * Print sense entry
     *
     * @param sensekey sensekey
     * @param offset   offset
     * @param senseNum sense number (index)
     * @param tagCount tag count
     * @param ps       print stream
     * ```
     * sense_key
     * synset_offset
     * sense_number
     * tag_cnt
     * ```
     */
    private fun printSenseEntry(sensekey: String, offset: Long, senseNum: Int, tagCount: Int, ps: PrintStream) {
        val line = "$sensekey ${offsetFormat(offset)} $senseNum $tagCount"
         ps.println(line)
    }
}
