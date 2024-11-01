/*
 * Copyright (c) 2022-2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.Sense
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object WN31Index {

    /**
     * Resource file
     */
    private const val RES = "/index.sense.31"

    /**
     * Sensekey to index map
     */
    private val SK2INDEX = readIndexes()

    /**
     * Sensekey comparator based on the sensekey-to-index map
     */
    val WN31_SK_COMPARATOR: Comparator<String> = Comparator.comparing { SK2INDEX[it]!! }

    /**
     * Sense comparator based on the sensekey-to-index map
     */
    @Suppress("unused")
    private val WN31_SENSE_ORDER: Comparator<Sense> = Comparator.comparing(Sense::senseKey, WN31_SK_COMPARATOR)

    /**
     * Sense comparator based on the 3.1 sensekey-to-index map. This comparator does not define a total order. It returns 0 when the sensekeys are not defined in 3.1
     */
    val SENSE_ORDER: Comparator<Sense> = Comparator { s1: Sense, s2: Sense ->
        val i1 = SK2INDEX[s1.senseKey]
        val i2 = SK2INDEX[s2.senseKey]
        if (i1 == null && i2 == null) {
            return@Comparator 0 // fail, to be chained with thenCompare
        }
        if (i1 != null && i2 == null) {
            return@Comparator -1 // non-null before null
        }
        if (i1 == null /* && i2 != null */) {
            return@Comparator 1 // null after non-null
        }
        val cmp = i1.compareTo(i2!!)
        // assert(cmp != 0) { "Senses have equal indexes $s1 $s2" }
        cmp
    }

    /**
     * Used in version 1
     * Flawed if one is null, the other is not
     */
    @Suppress("unused")
    val SENSE_ORDER1: Comparator<Sense> = Comparator { s1: Sense, s2: Sense ->
        val i1 = SK2INDEX[s1.senseKey]
        val i2 = SK2INDEX[s2.senseKey]
        if (i1 == null || i2 == null) {
            return@Comparator 0 // fail, to be chained with thenCompare
        }
        val cmp = i1.compareTo(i2)
        // assert(cmp != 0) { "Senses have equal indexes $s1 $s2" }
        cmp
    }

    /**
     * Read indexes from resource index.sense.31, a stripped-down version of index sense with three columns
     * sensekey index tagcount
     * sense_number or index is a decimal integer indicating the sense number of the word, within the part of speech encoded in sense_key , in the WordNet database.
     * ```
     * How sense numbers are assigned [from wndb(5WN) Sense Numbers section]
     * Senses in WordNet are generally ordered from most to least frequently used, with the most common sense numbered 1.
     * Frequency of use is determined by the number of times a sense is tagged in the various semantic concordance texts.
     * Senses that are not semantically tagged follow the ordered senses. The tagsense_cnt field for each entry in the index.pos files indicates how many of the senses in the list have been tagged.
     * ```
     * @return sensekey-to-index map
     */
    private fun readIndexes(): Map<String, Int> {
        val url = checkNotNull(WN31Index::class.java.getResource(RES))
        val map: MutableMap<String, Int> = HashMap()
        try {
            BufferedReader(InputStreamReader(url.openStream(), StandardCharsets.UTF_8)).use { reader ->
                reader.useLines { lines ->
                    lines.forEach { line ->
                        if (line.isNotEmpty()) {
                            try {
                                val fields = line.split("\\s".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                                val senseKey = fields[0] // .trimEnd { it == ':' }
                                val index = fields[1].toInt()
                                map[senseKey] = index
                            } catch (e: RuntimeException) {
                                Tracing.psErr.println("[E] reading at line '$line' $e")
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Tracing.psErr.println("[E] reading WN31 index $e")
        }
        return map
    }

    fun getWN31SenseIndex(sense: Sense): Int? {
        return SK2INDEX[sense.senseKey]
    }
}
