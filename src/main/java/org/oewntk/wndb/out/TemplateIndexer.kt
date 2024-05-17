/*
 * Copyright (c) 2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.Sense
import java.io.PrintStream

/**
 * Template indexer (sentidx.vrb)
 */
class TemplateIndexer {

    /**
     * Make index
     *
     * @param ps         print stream
     * @param sensesById senses by id
     */
    fun makeIndex(ps: PrintStream, sensesById: Map<String, Sense>) {
        val n = sensesById
            .filter { (_, sense) -> !sense.verbTemplates.isNullOrEmpty() }
            .onEach { (sensekey, sense) ->
                val templates = sense.verbTemplates!!.joinToString(separator = ",")
                val line = "$sensekey $templates"
                ps.println(line)
            }
            .count()
        Tracing.psInfo.println("Verb template references: $n senses")
    }
}
