/*
 * Copyright (c) 2022-2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.Sense
import org.oewntk.model.SenseGroupings

/**
 * Sense comparators
 */
object SenseComparator {

    /**
     * Tail comparator, after head comparators fail
     */
    val WNDB_SENSE_ORDER_TAIL: Comparator<Sense> = Comparator { s1: Sense, s2: Sense ->

        // senses are equal
        if (s1 == s2) {
            return@Comparator 0
        }

        // type a before s
        if (s1.type != s2.type) {
            return@Comparator s1.type.compareTo(s2.type)
        }

        // index in lex
        val cmpLexIndex = s1.lexIndex.compareTo(s2.lexIndex)
        if (cmpLexIndex != 0) {
            // lex indexes differ
            return@Comparator cmpLexIndex
        }

        // lower-cased lemmas
        val cmpLowerCase = s1.lCLemma.compareTo(s2.lCLemma)
        if (cmpLowerCase != 0) {
            // different lemmas regardless of case
            return@Comparator cmpLowerCase
        }

        // same lemmas, cased-lemmas
        val cmpCase = s1.lemma.compareTo(s2.lemma)
        if (cmpCase != 0) {
            // different cased lemmas, upper-case last
            return@Comparator -cmpCase
        }
        // same cased lemmas

        // compare sensekey
        val cmpSensekey = s1.senseKey.compareTo(s2.senseKey)
        if (cmpSensekey != 0) {
            return@Comparator cmpSensekey
        }
        // total order defined above, so this should not be reached
        throw IllegalArgumentException("$s1-$s2")
    }

    /**
     * Tail comparator, after head comparators fail
     */
    @Suppress("unused")
    val WNDB_SENSE_ORDER_TAIL1: Comparator<Sense> = Comparator { s1: Sense, s2: Sense ->

        // senses are equal
        if (s1 == s2) {
            return@Comparator 0
        }

        // type a before s
        if (s1.type != s2.type) {
            return@Comparator s1.type.compareTo(s2.type)
        }

        // index in lex
        val cmpLexIndex = s1.lexIndex.compareTo(s2.lexIndex)
        if (cmpLexIndex != 0) {
            // lex indexes differ
            return@Comparator cmpLexIndex
        }

        // different lemmas, upper-case first
        val lemma1 = s1.lemma
        val lemma2 = s2.lemma
        val cmpCase = lemma1.compareTo(lemma2)
        if (cmpCase != 0) {
            // different lemmas, upper-case first
            return@Comparator cmpCase
        }

        // compare sensekey
        val cmpSensekey = s1.senseKey.compareTo(s2.senseKey)
        if (cmpSensekey != 0) {
            return@Comparator cmpSensekey
        }
        // total order defined above, so this should not be reached
        throw IllegalArgumentException("$s1-$s2")
    }

    /**
     * Define a total order of senses
     */
    val WNDB_SENSE_ORDER: Comparator<Sense> = SenseGroupings.BY_DECREASING_TAGCOUNT
        .thenComparing(WN31Index.SENSE_ORDER)
        .thenComparing(WNDB_SENSE_ORDER_TAIL)
}
