/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.oewntk.model.Sense;
import org.oewntk.model.TagCount;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GrindTagCounts
{
	public void makeTagCountRev(final PrintStream ps, final Map<String, Sense> sensesById)
	{
		long n = 0;
		for (Map.Entry<String, Sense> entry : sensesById.entrySet())
		{
			String sensekey = entry.getKey();
			Sense sense = entry.getValue();
			TagCount tagCount = sense.getTagCount();
			if (tagCount != null)
			{
				String line = String.format("%s %d %d", sensekey, sense.getLexIndex(), tagCount.getCount());
				ps.println(line);
				n++;
			}
		}
		Tracing.psInfo.printf("Tag counts reverse: %d%n", n);
	}

	public void makeTagCount(final PrintStream ps, final Map<String, Sense> sensesById)
	{
		List<String> lines = new ArrayList<>();
		for (Map.Entry<String, Sense> entry : sensesById.entrySet())
		{
			String sensekey = entry.getKey();
			Sense sense = entry.getValue();
			TagCount tagCount = sense.getTagCount();
			if (tagCount != null)
			{
				String line = String.format("%d %s %d", tagCount.getCount(), sensekey, sense.getLexIndex());
				lines.add(line);
			}
		}
		lines.sort((l1, l2) -> {
			String field1 = l1.split("\\s")[0];
			String field2 = l2.split("\\s")[0];
			int i1 = Integer.parseInt(field1);
			int i2 = Integer.parseInt(field2);
			return -Integer.compare(i1, i2);
		});
		long n = 0;
		for (String line : lines)
		{
			ps.println(line);
			n++;
		}
		Tracing.psInfo.printf("Tag counts: %d%n", n);
	}
}
