/*
 * Copyright (c) 2022. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.oewntk.model.Sense;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WN31Index
{
	private static final String RES = "/index.sense.31";

	private static final Map<String, Integer> SK2INDEX = readIndexes();

	public static final Comparator<String> WN31_SK_COMPARATOR = Comparator.comparing(SK2INDEX::get);

	public static final Comparator<Sense> WN31_SENSE_ORDER = Comparator.comparing(Sense::getSensekey, WN31_SK_COMPARATOR);

	public static final Comparator<Sense> SENSE_ORDER = (s1, s2) -> {

		Integer i1 = SK2INDEX.get(s1.getSensekey());
		Integer i2 = SK2INDEX.get(s2.getSensekey());
		if (i1 == null || i2 == null)
		{
			return 0; // fail, to be chained with thenCompare
		}
		int cmp = i1.compareTo(i2);
		assert cmp != 0;
		return cmp;
	};

	public static Map<String, Integer> readIndexes()
	{
		Map<String, Integer> map = new HashMap<>();

		final URL url = WN31Index.class.getResource(RES);
		assert url != null;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.isEmpty())
				{
					continue;
				}

				try
				{
					String[] fields = line.split("\\s");
					String field1 = fields[0];
					Integer field2 = Integer.parseInt(fields[1]);
					map.put(field1, field2);
				}
				catch (final RuntimeException e)
				{
					Tracing.psErr.println("[E] reading at line '" + line + "' " + e);
				}
			}
		}
		catch (IOException e)
		{
			Tracing.psErr.println("[E] reading WN31 index " + e);
		}
		return map;
	}

	public static void main(String[] args)
	{
		var list = List.of("eight%1:06:00::", "eight%1:14:00::", "eight%1:23:00::");
		list.stream().sorted(WN31_SK_COMPARATOR).forEach(System.out::println);
	}
}
