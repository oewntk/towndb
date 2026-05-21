/*
 * Copyright (c) 2021-2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.*

/**
 * Class that produces the synset line from model and synsetId
 *
 * @param flags flags
 *
 * @author Bernard Bou
 */
class LineProducer(
    private val flags: Int,
) : (CoreModel, String) -> String {

    override fun invoke(model: CoreModel, synsetId: SynsetId): String {

        // Compute synset offsets
        val offsets = GrindOffsets(model.synsets, model.lexResolver, model.synsetResolver, model.senseResolver, flags).compute()

        // Get synset
        val synset = model.synsetResolver(synsetId)
        val offset = offsets[synsetId]!!
        require(offsets.containsValue(offset)) { "$offset is not a valid offset" }

        // Produce line
        return data(synset, offset, model.synsets, model.lexResolver, model.synsetResolver, model.senseResolver, offsets, flags)
    }

    companion object {

        /**
         * Grind data for this synset
         *
         * @param synset synset
         * @param synsets all synsets
         * @param offset offset
         * @param lexResolver lex resolver from lemma
         * @param synsetResolver synset resolver from id
         * @param senseResolver sense resolver from id
         * @param offsets offsets mapped by synsetId
         * @param flags flags
         * @return line
         */
        fun data(
            synset: Synset, offset: Long,
            synsets: Collection<Synset>,
            lexResolver: (Lemma) -> Collection<Lex>,
            synsetResolver: (SynsetId) -> Synset,
            senseResolver: (SenseKey) -> Sense,
            offsets: Map<String, Long>, flags: Int,
        ): String {
            val factory = GrindSynsets(synsets, lexResolver, synsetResolver, senseResolver, offsets, flags)
            return factory.getData(synset, offset)
        }
    }
}
