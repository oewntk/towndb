/*
 * Copyright (c) 2021-2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.junit.BeforeClass
import org.junit.Test
import org.oewntk.model.Lex
import org.oewntk.model.Sense
import org.oewntk.model.TagCount
import org.oewntk.model.Tracing
import org.oewntk.wndb.out.Formatter.joinToStringWithCount
import kotlin.test.assertEquals

class TestFormat {

    private val lexPythonCapitalized = Lex("Python", "n", null)
    private val lexPython = Lex("python", "n", null)
    private val senseBoa = Sense("python%1:05:00::", lexPython, 'n', 0, "01746246-n", null, null, null, null).apply { tagCount = TagCount(1, 3) }
    private val senseSoothsayer = Sense("python%1:18:01::", lexPython, 'n', 1, "10516512-n", null, null, null, null)
    private val senseDragon = Sense("python%1:18:00::", lexPythonCapitalized, 'n', 0, "09524330-n", null, null, null, null)
    private val senseLanguage = Sense("python%1:10:01::", lexPythonCapitalized, 'n', 1, "83541804-n", null, null, null, null)
    private val senses = listOf(senseBoa, senseSoothsayer, senseDragon, senseLanguage)

    private val seq = listOf("a", 'b', "c", "d")

    @Test
    fun testJoinWithCount() {
        val r = seq.joinToStringWithCount()
        ps.println(r)
        assertEquals("4 a b c d", r)
    }

    @Test
    fun testJoinWithCount2() {
        val r = seq.joinToStringWithCount(countFormat = "%02d")
        ps.println(r)
        assertEquals("04 a b c d", r)
    }

    @Test
    fun testJoinWithCountAndTransform() {
        val r = seq.joinToStringWithCount(countFormat = "%02d") { "*$it*" }
        ps.println(r)
        assertEquals("04 *a* *b* *c* *d*", r)
    }

    @Test
    fun testJoinWithCountAndCountPrefix() {
        val r = seq.joinToStringWithCount(countFormat = "%04d", countSeparator = " # ") { "+$it-" }
        ps.println(r)
        assertEquals("0004 # +a- +b- +c- +d-", r)
    }

    @Test
    fun testJoinWithCountEmpty() {
        val r = emptyList<Any>().joinToStringWithCount(countFormat = "%04d", countSeparator = " # ") { "+$it-" }
        ps.println(r)
        assertEquals("0000", r)
    }

    @Test
    fun testJoinSensesWithCountAEmpty() {
        val r = senses.joinToStringWithCount(countFormat = "%04d", countSeparator = " ") { it.synsetId.trimEnd('-', 'n', 'v', 'a', 'r', 's') }
        ps.println(r)
        assertEquals("0004 01746246 10516512 09524330 83541804", r)
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
