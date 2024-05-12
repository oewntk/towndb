/*
 * Copyright (c) 2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.Category
import org.oewntk.model.Lex
import org.oewntk.model.Sense
import org.oewntk.model.Synset
import java.nio.charset.StandardCharsets

/**
 * This class computes file offsets that serve as synset id in the WNDB format. It does so by iterating over synsets and yielding a dummy line string of
 * the same length as the final string. The offset counter is moved by the line's length.
 *
 * @param lexesByLemma lexes mapped by lemma
 * @param synsetsById  synsets mapped by id
 * @param sensesById   senses mapped by id
 * @param flags        flags
 *
 * @author Bernard Bou
 */
class GrindOffsets(
    lexesByLemma: Map<String, Collection<Lex>>,
    synsetsById: Map<String, Synset>,
    sensesById: Map<String, Sense>,
    flags: Int,
) :
    SynsetProcessor(lexesByLemma, synsetsById, sensesById, { 0L }, flags) {

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
    private fun compute(posFilter: Char, offsets: MutableMap<String, Long>) {
        var offset = Formatter.OEWN_HEADER.toByteArray(StandardCharsets.UTF_8).size.toLong()

        // iterate synsets
        synsetsById
            .filter { (_, synset) -> synset.partOfSpeech == posFilter }
            .forEach { (synsetId, synset) ->
                val data = getData(synset, dummyOfs)
                offsets[synsetId] = offset
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
        compute(Data.NOUN_POS_FILTER, offsets)
        compute(Data.VERB_POS_FILTER, offsets)
        compute(Data.ADJ_POS_FILTER, offsets)
        compute(Data.ADV_POS_FILTER, offsets)
        return offsets
    }

    // I M P L E M E N T A T I O N

    private val dummyOfs = offsetFunction.invoke("")

    @Throws(CompatException::class)
    override fun buildSenseRelation(type: String, category: Category, sourceMemberNum: Int, targetSense: Sense, targetSynset: Synset, targetSynsetId: String): Data.Relation {
        val targetType = targetSynset.type
        val pointerCompat = (flags and Flags.POINTER_COMPAT) != 0
        return Data.Relation(type, category, targetType, dummyOfs, DUMMY_NUM, DUMMY_NUM, pointerCompat)
    }

    override fun buildLexfileNum(synset: Synset): Int {
        return DUMMY_NUM
    }

    companion object {

        private const val DUMMY_NUM = 0
    }
}
