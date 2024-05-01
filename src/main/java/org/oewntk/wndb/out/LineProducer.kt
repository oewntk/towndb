/*
 * Copyright (c) 2021-2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.CoreModel
import org.oewntk.model.Lex
import org.oewntk.model.Sense
import org.oewntk.model.Synset

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

    override fun invoke(model: CoreModel, synsetId: String): String {

        // Compute synset offsets
        val offsets = GrindOffsets(model.lexesByLemma!!, model.synsetsById!!, model.sensesById!!, flags).compute()

        // Get synset
        val synset = model.synsetsById!![synsetId]!!
        val offset = offsets[synsetId]!!
        require(offsets.containsValue(offset)) { "$offset is not a valid offset" }

        // Produce line
        return data(synset, offset, model.lexesByLemma!!, model.synsetsById!!, model.sensesById!!, offsets, flags)
    }

    companion object {

        /**
         * Grind data for this synset
         *
         * @param synset       synset
         * @param offset       offset
         * @param lexesByLemma lexes mapped by lemma
         * @param synsetsById  synset elements mapped by id
         * @param sensesById   sense elements mapped by id
         * @param offsets      offsets mapped by synsetId
         * @param flags        flags
         * @return line
         */
        fun data(
            synset: Synset, offset: Long,  
            lexesByLemma: Map<String, Collection<Lex>>,  
            synsetsById: Map<String, Synset>,  
            sensesById: Map<String, Sense>,  
            offsets: Map<String, Long>, flags: Int,
        ): String {
            val factory = GrindSynsets(lexesByLemma, synsetsById, sensesById, offsets, flags)
            return factory.getData(synset, offset)
        }
    }
}
