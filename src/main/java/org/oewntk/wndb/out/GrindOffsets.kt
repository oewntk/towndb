/*
 * Copyright (c) 2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.*
import java.nio.charset.StandardCharsets

/**
 * This class computes file offsets that serve as synset id in the WNDB format. It does so by iterating over synsets and yielding a dummy line string of
 * the same length as the final string. The offset counter is moved by the line's length.
 *
 * @property synsets synsets
 * @param lexResolver lex resolver from lemma
 * @param synsetResolver  synset resolver from  id
 * @param senseResolver   sens resolver from  id
 * @param flags        flags
 *
 * @author Bernard Bou
 */
class GrindOffsets(
    val synsets: Collection<Synset>,
    lexResolver: (Lemma) -> Collection<Lex>,
    synsetResolver: (SynsetId) -> Synset,
    senseResolver: (SenseKey) -> Sense,
    flags: Int,
    val verbose: Boolean = false,
) :
    SynsetProcessor(lexResolver, synsetResolver, senseResolver, { 0L }, flags,) {

    /**
     * Log things on the first pass
     */
    override fun log(): Boolean {
        return false
    }

    /**
     * Compute synset offsets
     *
     * @param posFilter     selection of synsets
     * @param offsets       result map
     */
    private fun compute(posFilter: PartOfSpeech, offsets: MutableMap<String, Long>) {
        var offset = Formatter.OEWN_HEADER.toByteArray(StandardCharsets.UTF_8).size.toLong()

        // iterate synsets
        synsets
            .filter { it.partOfSpeech == posFilter }
            .forEach { synset ->
                val data = getData(synset, dummyOfs)
                offsets[synset.synsetId] = offset
                offset += data.toByteArray(StandardCharsets.UTF_8).size.toLong()
            }
    }

    /**
     * Compute offsets mapped by synsetId
     *
     * @return map of offsets by synsetId
     */
    fun compute(): Map<String, Long> {
        val offsets: MutableMap<String, Long> = HashMap()
        compute(PartOfSpeech.N, offsets)
        compute(PartOfSpeech.V, offsets)
        compute(PartOfSpeech.A, offsets)
        compute(PartOfSpeech.R, offsets)
        return offsets
    }

    // I M P L E M E N T A T I O N

    private val dummyOfs = offsetFunction.invoke("")

    @Throws(CompatException::class)
    override fun buildSenseRelation(rel: String, type: SynsetType, sourceMemberNum: Int, targetSense: Sense, targetSynset: Synset, targetSynsetId: String): Data.Relation {
        val targetType = targetSynset.type
        val pointerCompat = (flags and Flags.POINTER_COMPAT) != 0
        return Data.Relation(rel, type, targetType, dummyOfs, DUMMY_NUM, DUMMY_NUM, pointerCompat)
    }

    override fun buildLexfileNum(synset: Synset): Int {
        return DUMMY_NUM
    }

    companion object {

        private const val DUMMY_NUM = 0
    }
}
