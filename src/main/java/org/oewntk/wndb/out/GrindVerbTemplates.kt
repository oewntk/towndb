/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
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
		var n: Long = 0
		for ((id, verbTemplate) in verbTemplatesById) {
			val line = String.format("%s %s", id, verbTemplate.template)
			ps.println(line)
			n++
		}
		Tracing.psInfo.printf("Verb templates: %d%n", n)
	}
}
