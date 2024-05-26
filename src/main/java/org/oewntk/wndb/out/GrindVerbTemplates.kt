/*
 * Copyright (c) 2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.VerbTemplate
import java.io.PrintStream

/**
 * Grind verb templates (sents.vrb)
 */
class GrindVerbTemplates {

    /**
     * Grind verb templates
     *
     * @param ps                print stream
     * @param verbTemplatesById verb templates by id
     */
    fun makeTemplates(ps: PrintStream, verbTemplatesById: Map<Int, VerbTemplate>) {
        val n = verbTemplatesById
            .onEach { (verbTemplateId, verbTemplate) ->
                val line = "$verbTemplateId ${verbTemplate.template}"
                ps.println(line)
            }
            .count()
        Tracing.psInfo.println("Verb templates: $n")
    }
}
