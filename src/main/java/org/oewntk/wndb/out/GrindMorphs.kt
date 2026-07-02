/*
 * Copyright (c) 2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.Lex
import org.oewntk.model.PartOfSpeech
import java.io.PrintStream

/**
 * This class produces the {noun|verb|adj|adv}.exc files
 *
 * @author Bernard Bou
 */
class GrindMorphs {

    /**
     * Make morph files
     * better good
     * better well
     *
     * @param ps           print stream
     * @param lexes        lexes
     * @param posFilter    filter selecting lexes
     * @return count
     */
    fun makeMorph1(ps: PrintStream, lexes: Collection<Lex>, posFilter: PartOfSpeech): Int {

        return lexes
            .asSequence()
            .filter { lex -> lex.partOfSpeech == posFilter }
            .filter { lex -> lex.forms != null }
            .flatMap { lex ->
                lex.forms!!.map { form -> form to lex.lemma }
            }
            .sortedBy { it.first }
            .distinct()
            .onEach { ps.println("${it.first.replace(' ', '_')} ${it.second.replace(' ', '_')}") } // form lemma
            .count()
    }

    /**
     * Make morph files
     * better good well
     * @param ps           print stream
     * @param lexes        lexes
     * @param posFilter    filter selecting lexes
     * @return count
     */
    fun makeMorph(ps: PrintStream, lexes: Collection<Lex>, posFilter: PartOfSpeech): Int {

        return lexes
            .asSequence()
            .filter { lex -> lex.partOfSpeech == posFilter }
            .filter { lex -> lex.forms != null }
            .flatMap { lex ->
                lex.forms!!.map { form -> form to lex.lemma }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.distinct() }
            .asSequence()
            .sortedBy { it.key }
            .distinct()
            .onEach { ps.println("${it.key.replace(' ', '_')} ${it.value.joinToString(separator = " ") { it.replace(' ', '_') }}") }
            .count()
    }
}
