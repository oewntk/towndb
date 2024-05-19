/*
 * Copyright (c) 2021-2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import junit.framework.TestCase.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.oewntk.model.*
import org.oewntk.model.Tracing
import org.oewntk.wndb.out.SenseComparator.WNDB_SENSE_ORDER2
import org.oewntk.wndb.out.SenseComparator.WNDB_SENSE_ORDER_TAIL
import org.oewntk.wndb.out.WN31Index.WN31_SK_COMPARATOR

class TestSenseOrder {

    // C O M P A R A T O R S

    private val WNDB_SENSE_ORDER_BY_TAGCOUNT: Comparator<Sense> = SenseGroupings.BY_DECREASING_TAGCOUNT

    private val WNDB_SENSE_ORDER_BY_WN31: Comparator<Sense> = WN31Index.SENSE_ORDER

    private val WNDB_SENSE_ORDER_BY_TAIL: Comparator<Sense> = WNDB_SENSE_ORDER_TAIL

    private val comparators = arrayOf(
        WNDB_SENSE_ORDER_BY_TAGCOUNT,
        WNDB_SENSE_ORDER_BY_WN31,
        WNDB_SENSE_ORDER_BY_TAIL,
        WNDB_SENSE_ORDER2
    )

    private val comparatorNames = mapOf(
        WNDB_SENSE_ORDER2 to "global",
        WNDB_SENSE_ORDER_BY_TAGCOUNT to "tagcount",
        WNDB_SENSE_ORDER_BY_WN31 to "wn31",
        WNDB_SENSE_ORDER_BY_TAIL to "tail"
    )

    private val Comparator<Sense>.short
        get() = comparatorNames[this]

    // S E N S E S

    // Python:
    //  n:
    //      pronunciation:
    //          - value: ˈpaɪθən
    //              variety: GB
    //          - value: ˈpaɪθɑːn
    //              variety: US
    //      sense:
    //          - id: 'python%1:18:00::'
    //              synset: 09524330-n
    //          - id: 'python%1:10:01::'
    //              synset: 83541804-n

    // python:
    //  n:
    //      pronunciation:
    //          - value: ˈpaɪθən
    //              variety: GB
    //          - value: ˈpaɪθɑːn
    //              variety: US
    //      sense:
    //          - id: 'python%1:05:00::'
    //              synset: 01746246-n
    //          - id: 'python%1:18:01::'
    //              synset: 10516512-n

    // "python%1:05:00::": {num: 1, cnt: 3}

    private val defs = mapOf(
        "01746246-n" to "boa",
        "10516512-n" to "soothsayer",
        "09524330-n" to "Dragon",
        "83541804-n" to "Language",
    )

    val offsets = mapOf(
        "01746246-n" to "01772693",
        "10516512-n" to "10675823",
        "09524330-n" to "15554324",
        "83541804-n" to "09662231",
    )

    private val Sense.short
        get() = defs[this.synsetId]

    private val lexPythonCapitalized = Lex("Python", "n")
    private val lexPython = Lex("python", "n")
    private val senseBoa = Sense("python%1:05:00::", lexPython, 'n', 0, "01746246-n").apply { tagCount = TagCount(1, 3) }
    private val senseSoothsayer = Sense("python%1:18:01::", lexPython, 'n', 1, "10516512-n")
    private val senseDragon = Sense("python%1:18:00::", lexPythonCapitalized, 'n', 0, "09524330-n")
    private val senseLanguage = Sense("python%1:10:01::", lexPythonCapitalized, 'n', 1, "83541804-n")
    private val senses = listOf(senseBoa, senseSoothsayer, senseDragon, senseLanguage)

    private fun testSortWith(comparator: Comparator<Sense>): List<Sense> {
        return senses
            .asSequence()
            .shuffled()
            .sortedWith(comparator)
            .onEach {
                ps.println(it.short)
            }
            .toList()
    }

    private fun testCompareWith(sense1: Sense, sense2: Sense, comparator: Comparator<Sense>) {
        val c = comparator.compare(sense1, sense2)
        val s: Char = when {
            c < 0 -> '<'
            c > 0 -> '>'
            else  -> '?'
        }
        ps.println("compare ${sense1.short} $s ${sense2.short} by ${comparator.short}")
    }

    private fun testCompareWithAllComparatrrs() {
        testCompareWith(*comparators)
    }

    private fun testCompareWith(vararg comparators: Comparator<Sense>) {
        senses
            .asSequence()
            .map { it to senses - it }
            .forEach {
                it.second.forEach { alt ->
                    comparators.forEach { comparator ->
                        testCompareWith(it.first, alt, comparator)
                    }
                }
                ps.println()
            }
    }

    @Test
    fun testWn31Index() {
        senses
            .forEach {
                val index = WN31Index.getWN31SenseIndex(it)
                ps.println("${it.short} -> WN31 index $index")
            }
    }

    @Test
    fun testCompareLanguageSoothsayer() {
        testCompareWith(senseLanguage, senseSoothsayer, WNDB_SENSE_ORDER_BY_TAGCOUNT)
        testCompareWith(senseLanguage, senseSoothsayer, WNDB_SENSE_ORDER_BY_WN31)
        testCompareWith(senseLanguage, senseSoothsayer, WNDB_SENSE_ORDER_BY_TAIL)
        testCompareWith(senseLanguage, senseSoothsayer, WNDB_SENSE_ORDER2)
    }

    @Test
    fun testCompareLanguageDragon() {
        testCompareWith(senseLanguage, senseDragon, WNDB_SENSE_ORDER_BY_TAGCOUNT)
        testCompareWith(senseLanguage, senseDragon, WNDB_SENSE_ORDER_BY_WN31)
        testCompareWith(senseLanguage, senseDragon, WNDB_SENSE_ORDER_BY_TAIL)
        testCompareWith(senseLanguage, senseDragon, WNDB_SENSE_ORDER2)
    }

    // C O M P O U N D  C O M P A R A T O R

    @Test
    fun testCompareWithAll() {
        testCompareWith(*comparators)
    }

    @Test
    fun testCompareWithWNDBSenseOrder() {
        testCompareWith(WNDB_SENSE_ORDER2)
    }

    @Test
    fun testSortBySenseOrder() {
        testSortWith(WNDB_SENSE_ORDER2)
    }

    @Test
    fun testSortWithSenseOrderExpected() {
        val order = testSortWith(WNDB_SENSE_ORDER2).map { it.short }
        assertEquals(listOf("boa", "soothsayer", "Dragon", "Language"), order)
    }

    // S P E C I F I  C   C O M P A R A T O R S

    @Test
    fun testSortWithWN31() {
        testSortWith(WNDB_SENSE_ORDER_BY_WN31)
    }

    @Test
    fun testCompareByWN31() {
        testCompareWith(WNDB_SENSE_ORDER_BY_WN31)
    }

    @Test
    fun testSortWithTagCount() {
        testSortWith(WNDB_SENSE_ORDER_BY_TAGCOUNT)
    }

    @Test
    fun testCompareByTagCount() {
        testCompareWith(WNDB_SENSE_ORDER_BY_TAGCOUNT)
    }

    @Test
    fun testSortWithTail() {
        testSortWith(WNDB_SENSE_ORDER_BY_TAIL)
    }

    @Test
    fun testCompareByTail() {
        testCompareWith(WNDB_SENSE_ORDER_BY_TAIL)
    }

    @Test
    fun testSortBySenseIndex31() {
        val sorted = listOf("eight%1:06:00::", "eight%1:14:00::", "eight%1:23:00::")
            .sortedWith(WN31_SK_COMPARATOR)
            .toList()
        sorted
            .forEach { println(it) }

        // eight%1:06:00:: 3 0
        // eight%1:14:00:: 2 0
        // eight%1:23:00:: 1 0
        assertEquals(listOf("eight%1:23:00::", "eight%1:14:00::", "eight%1:06:00::"), sorted)
    }

    companion object {

        private val ps = if (!System.getProperties().containsKey("SILENT")) Tracing.psInfo else Tracing.psNull

        @Suppress("EmptyMethod")
        @JvmStatic
        @BeforeClass
        fun init() {
        }
    }
}
