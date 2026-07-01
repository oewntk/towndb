/*
 * Copyright (c) 2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.Lex
import org.oewntk.model.PartOfSpeech
import java.io.PrintStream

/**
 * This class produces the index.{noun|verb|adj|adv}.exc files
 *
 * @author Bernard Bou
 */
class GrindMorphs {

    /**
     * Make morph files
     *
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
                lex.forms!!
                    .asSequence()
                    .map { form -> "$form ${lex.lemma}" }
                    .sorted()
            }
            .sorted()
            .onEach { ps.println(it) }
            .count()
    }
}
