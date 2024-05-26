/*
 * Copyright (c) 2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.Lex
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
     * @param lexesByLemma lexes mapped by lemma
     * @param posFilter    filter selecting lexes
     * @return count
     */
    fun makeMorph(ps: PrintStream, lexesByLemma: Map<String, Collection<Lex>>, posFilter: Char): Int {

        return lexesByLemma.entries
            .asSequence()
            .flatMap { (lemma, lexes) ->
                lexes.asSequence()
                    .map { lex -> lemma to lex }
            }
            .filter { (_, lex) -> lex.partOfSpeech == posFilter }
            .filter { (_, lex) -> lex.forms != null }
            .flatMap { (lemma, lex) ->
                lex.forms!!
                    .asSequence()
                    .map { form -> "$form $lemma" }
            }
            .sorted()
            .distinct()
            .onEach { ps.println(it) }
            .count()
    }
}
