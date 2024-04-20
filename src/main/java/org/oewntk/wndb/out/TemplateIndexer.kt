/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.oewntk.model.Sense;

import java.io.PrintStream;
import java.util.Map;

/**
 * Template indexer (sentidx.vrb)
 */
public class TemplateIndexer
{
	/**
	 * Make index
	 *
	 * @param ps         print stream
	 * @param sensesById senses by id
	 */
	public void makeIndex(final PrintStream ps, final Map<String, Sense> sensesById)
	{
		long n = 0;
		for (Map.Entry<String, Sense> entry : sensesById.entrySet())
		{
			String sensekey = entry.getKey();
			Sense sense = entry.getValue();
			Integer[] verbTemplateIds = sense.getVerbTemplates();
			if (verbTemplateIds != null && verbTemplateIds.length > 0)
			{
				String line = String.format("%s %s", sensekey, Formatter.join(verbTemplateIds, ","));
				ps.println(line);
				n++;
			}
		}
		Tracing.psInfo.printf("Verb template references: %d senses%n", n);
	}
}
