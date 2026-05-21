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
     * @param ps     print stream
     * @param senses senses by id
     */
    fun makeIndex(ps: PrintStream, senses: Collection<Sense>) {
        val n = senses
            .filter { sense -> !sense.verbTemplates.isNullOrEmpty() }
            .onEach { sense ->
                val templates = sense.verbTemplates!!.joinToString(separator = ",")
                val line = "${sense.senseKey} $templates"
                ps.println(line)
            }
            .count()
        Tracing.psInfo.println("Verb template references: $n senses")
    }
}
