/*
 * Copyright (c) 2021-2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.junit.BeforeClass
import org.junit.Test
import org.oewntk.model.*
import org.oewntk.model.Tracing
import org.oewntk.wndb.out.Formatter.intFormat2
import org.oewntk.wndb.out.Formatter.joinToStringWithCount
import java.io.PrintStream
import kotlin.test.assertEquals

class TestFormat {

    private val lexPythonCapitalized = Lex("Python", "n")
    private val lexPython = Lex("python", "n")
    private val senseBoa = Sense("python%1:05:00::", lexPython.key, "01746246-n", indexInLex = 0).apply { tagCount = 3 }
    private val senseSoothsayer = Sense("python%1:18:01::", lexPython.key, "10516512-n", indexInLex = 1)
    private val senseDragon = Sense("python%1:18:00::", lexPythonCapitalized.key, "09524330-n", indexInLex = 0)
    private val senseLanguage = Sense("python%1:10:01::", lexPythonCapitalized.key, "83541804-n", indexInLex = 1)
    private val senses = listOf(senseBoa, senseSoothsayer, senseDragon, senseLanguage)

    private val seq = listOf("a", 'b', "c", "d")

    private fun intFormat4(int: Int): String {
        return "%04d".format(int)
    }

    @Test
    fun testJoinWithCount() {
        val r = seq.joinToStringWithCount(countFormat = Int::toString)
        ps.println(r)
        assertEquals("4 a b c d", r)
    }

    @Test
    fun testJoinWithCount2() {
        val r = seq.joinToStringWithCount(countFormat = ::intFormat2)
        ps.println(r)
        assertEquals("04 a b c d", r)
    }

    @Test
    fun testJoinWithCountAndTransform() {
        val r = seq.joinToStringWithCount(countFormat = ::intFormat2) { "*$it*" }
        ps.println(r)
        assertEquals("04 *a* *b* *c* *d*", r)
    }

    @Test
    fun testJoinWithCountAndCountPrefix() {
        val r = seq.joinToStringWithCount(countFormat = ::intFormat4, countSeparator = " # ") { "+$it-" }
        ps.println(r)
        assertEquals("0004 # +a- +b- +c- +d-", r)
    }

    @Test
    fun testJoinWithCountEmpty() {
        val r = emptyList<Any>().joinToStringWithCount(countFormat = ::intFormat4, countSeparator = " # ") { "+$it-" }
        ps.println(r)
        assertEquals("0000", r)
    }

    @Test
    fun testJoinSensesWithCountAEmpty() {
        val r = senses.joinToStringWithCount(countFormat = ::intFormat4, countSeparator = " ") { it.synsetId.trimEnd('-', 'n', 'v', 'a', 'r', 's') }
        ps.println(r)
        assertEquals("0004 01746246 10516512 09524330 83541804", r)
    }

    companion object {

        private val silent = !System.getProperties().containsKey("VERBOSE") && if (System.getProperties().containsKey("SILENT")) true
        else true

        private val ps: PrintStream = if (!silent) Tracing.psInfo else Tracing.psNull

        @Suppress("EmptyMethod")
        @JvmStatic
        @BeforeClass
        fun init() {
        }
    }
}
