/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.oewntk.model.VerbTemplate;

import java.io.PrintStream;
import java.util.Map;

/**
 * Grind verb templates (sents.vrb)
 */
public class GrindVerbTemplates
{
	/**
	 * Grind verb templates
	 *
	 * @param ps                print stream
	 * @param verbTemplatesById verb templates by id
	 */
	public void makeTemplates(final PrintStream ps, final Map<Integer, VerbTemplate> verbTemplatesById)
	{
		long n = 0;
		for (Map.Entry<Integer, VerbTemplate> entry : verbTemplatesById.entrySet())
		{
			Integer id = entry.getKey();
			VerbTemplate verbTemplate = entry.getValue();
			if (verbTemplate != null)
			{
				String line = String.format("%s %s", id, verbTemplate.template);
				ps.println(line);
				n++;
			}
		}
		Tracing.psInfo.printf("Verb templates: %d%n", n);
	}
}
