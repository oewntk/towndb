/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.oewntk.model.VerbTemplate;

import java.io.PrintStream;
import java.util.Map;

public class TemplateGrinder
{
	public void makeTemplates(final PrintStream ps, final Map<Integer, VerbTemplate> verbTemplatesById)
	{
		long n = 0;
		for (Map.Entry<Integer, VerbTemplate> entry : verbTemplatesById.entrySet())
		{
			Integer id = entry.getKey();
			VerbTemplate verbTemplate = entry.getValue();
			if (verbTemplate != null)
			{
				String line = String.format("%s %s", id, verbTemplate.getTemplate());
				ps.println(line);
				n++;
			}
		}
		System.err.printf("Verb templates %d%n", n);
	}
}
