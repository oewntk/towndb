/*
 * Copyright (c) 2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.*
import java.io.PrintStream
import java.nio.charset.StandardCharsets

/**
 * This class produces the 'data.{noun|verb|adj|adv}' files
 *
 * @param synsets synsets
 * @param lexResolver lex resolver from lemma
 * @param synsetResolver synset resolver from id
 * @param senseResolver sense resolver from id
 * @param offsetMap offsets by synset id
 * @param flags flags
 *
 * @author Bernard Bou
 */
class GrindSynsets(
    synsets: Collection<Synset>,
    lexResolver: (Lemma) -> Collection<Lex>,
    synsetResolver: (SynsetId) -> Synset,
    senseResolver: (SenseKey) -> Sense,
    offsetMap: Map<String, Long>,
    flags: Int,
    verbose: Boolean = false,
) : SynsetProcessor(synsets, lexResolver, synsetResolver, senseResolver, { offsetMap[it]!! }, flags, verbose = verbose) {

    /**
     * Derived classes diverge here to log things on the second pass only
     */
    override fun log(): Boolean {
        return true
    }

    /**
     * Make data
     *
     * @param ps print stream
     * @param synsets all synsets
     * @param synsetResolver from id
     * @param posFilter part-of-speech filter
     * @return number of synsets
     */
    fun makeData(ps: PrintStream, synsets: Collection<Synset>, synsetResolver: (SynsetId) -> Synset, posFilter: PartOfSpeech): Int {
        ps.print(Formatter.OEWN_HEADER)

        // iterate synsets
        var offset = Formatter.OEWN_HEADER.toByteArray(StandardCharsets.UTF_8).size.toLong()
        var previous: Synset? = null
        return synsets
            .filter { synset -> synset.partOfSpeech == posFilter }
            .onEach { synset ->
                val offset0: Long = offsetFunction.invoke(synset.synsetId)
                if (offset0 != offset) {
                    checkNotNull(previous)
                    val line = getData(previous, 0)
                    val line0 = GrindOffsets(synsets, lexResolver, synsetResolver, senseResolver, flags, verbose = verbose,).getData(previous, 0)
                    throw RuntimeException("miscomputed offset for ${synset.synsetId}\n[then]=$line0[now ]=$line")
                }
                val line = getData(synset, offset)
                ps.print(line)
                offset += line.toByteArray(StandardCharsets.UTF_8).size.toLong()
                previous = synset
            }
            .count()
    }
}
