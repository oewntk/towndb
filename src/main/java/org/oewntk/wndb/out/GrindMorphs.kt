/*
 * Copyright (c) 2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.Lex
import java.io.PrintStream
import java.util.*

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
     * @return morphs
     */
    fun makeMorph(ps: PrintStream, lexesByLemma: Map<String, Collection<Lex>>, posFilter: Char): Long {
        val lines: MutableSet<String> = TreeSet()

        // iterate lexes
        var n = 0
        for ((lemma, lexes) in lexesByLemma) {
            for (lex in lexes) {
                if (lex.partOfSpeech != posFilter) {
                    continue
                }

                val forms = lex.forms
                if (forms != null) {
                    for (form in forms) {
                        val line = "$form $lemma"
                        lines.add(line)
                        n++
                    }
                }
            }
        }
        for (line in lines) {
            ps.println(line)
        }
        return n.toLong()
    }
}
