/*
 * Copyright (c) 2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.Lex
import org.oewntk.model.Sense
import org.oewntk.model.Synset
import java.io.PrintStream
import java.nio.charset.StandardCharsets

/**
 * This class produces the 'data.{noun|verb|adj|adv}' files
 *
 * @param lexesByLemma lexes mapped by lemma
 * @param synsetsById  synsets mapped by id
 * @param sensesById   senses mapped by id
 * @param offsetMap    offsets by synset id
 * @param flags        flags
 *
 * @author Bernard Bou
 */
class GrindSynsets(
    lexesByLemma: Map<String, Collection<Lex>>,
    synsetsById: Map<String, Synset>,
    sensesById: Map<String, Sense>,
    offsetMap: Map<String, Long>,
    flags: Int,
) : SynsetProcessor(lexesByLemma, synsetsById, sensesById, { offsetMap[it]!! }, flags) {

    /**
     * Derived classes diverge here to log things on the second pass only
     */
    override fun log(): Boolean {
        return true
    }

    /**
     * Make data
     *
     * @param ps          print stream
     * @param synsetsById synsets mapped by id
     * @param posFilter   part-of-speech  filter
     * @return number of synsets
     */
    fun makeData(ps: PrintStream, synsetsById: Map<String, Synset>, posFilter: Char): Long {
        ps.print(Formatter.OEWN_HEADER)
        var offset = Formatter.OEWN_HEADER.toByteArray(StandardCharsets.UTF_8).size.toLong()
        var previous: Synset? = null

        // iterate synsets
        var n: Long = 0
        for ((id, synset) in synsetsById) {
            if (synset.partOfSpeech != posFilter) {
                continue
            }
            n++
            val offset0: Long = offsetFunction.invoke(id)
            if (offset0 != offset) {
                checkNotNull(previous)
                val line = getData(previous, 0)
                val line0 = GrindOffsets(lexesByLemma, synsetsById, sensesById, flags).getData(previous, 0)
                throw RuntimeException("miscomputed offset for $id\n[then]=$line0[now ]=$line")
            }
            val line = getData(synset, offset)
            ps.print(line)
            offset += line.toByteArray(StandardCharsets.UTF_8).size.toLong()
            previous = synset
        }
        return n
    }
}
