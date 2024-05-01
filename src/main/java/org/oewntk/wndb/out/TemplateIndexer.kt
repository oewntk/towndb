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
        var n: Long = 0
        for ((sensekey, sense) in sensesById) {
            val verbTemplateIds = sense.verbTemplates
            if (!verbTemplateIds.isNullOrEmpty()) {
                val templates = verbTemplateIds.joinToString( ",")
                val line = "$sensekey $templates"
                ps.println(line)
                n++
            }
        }
        Tracing.psInfo.printf("Verb template references: %d senses%n", n)
    }
}
