/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.oewntk.model.Sense;
import org.oewntk.model.TagCount;

import java.io.PrintStream;
import java.util.*;

/**
 * This class produces the 'index.sense' file
 */
public class SenseIndexer
{
	// sense_key synset_offset sense_number tag_cnt

	/**
	 * Format in data file
	 */
	private static final String SENSE_FORMAT = "%s %08d %d %d  ";

	static final Comparator<String> lexicalComparatorLowerFirst = (s1, s2) -> {
		int c = s1.compareToIgnoreCase(s2);
		if (c != 0)
		{
			return c;
		}
		return -s1.compareTo(s2);
	};

	static final Comparator<String> lexicalComparatorUpperFirst = (s1, s2) -> {
		int c = s1.compareToIgnoreCase(s2);
		if (c != 0)
		{
			return c;
		}
		return s1.compareTo(s2);
	};

	private static class Data
	{
		public final long offset;

		public final int senseNum;

		public final int tagCount;

		public Data(long offset, int senseNum, int tagCount)
		{
			this.offset = offset;
			this.senseNum = senseNum;
			this.tagCount = tagCount;
		}

		@Override
		public boolean equals(Object o)
		{
			if (this == o)
			{
				return true;
			}
			if (o == null || getClass() != o.getClass())
			{
				return false;
			}
			Data data = (Data) o;
			return offset == data.offset && senseNum == data.senseNum && tagCount == data.tagCount;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(offset, senseNum, tagCount);
		}
	}

	/**
	 * Synset offsets map indexed by synsetid key
	 */
	private final Map<String, Long> offsets;

	/**
	 * Constructor
	 *
	 * @param offsets synset offsets map indexed by synsetid key
	 */
	public SenseIndexer(Map<String, Long> offsets)
	{
		super();
		this.offsets = offsets;
	}

	/**
	 * Make 'index.sense'. Sensekeys are lower-cased. Multiple lines may have the same key, which makes binary search yield unpredictable (non-deterministic)
	 * results.
	 *
	 * @param ps         print stream
	 * @param sensesById senses mapped by id
	 */
	public void makeIndexLowerMultiKey(PrintStream ps, Map<String, Sense> sensesById)
	{
		SortedSet<String> lines = new TreeSet<>(lexicalComparatorUpperFirst);

		for (Map.Entry<String, Sense> entry : sensesById.entrySet())
		{
			// sense
			String sensekey = entry.getKey();
			String sensekeyLower = sensekey.toLowerCase();
			Sense sense = entry.getValue();

			// offset
			String synsetId = sense.getSynsetId();
			long offset = offsets.get(synsetId);

			// sense num, tag count
			TagCount tagCount = sense.getTagCount();
			int count = tagCount == null ? 0 : tagCount.getCount();
			int senseNum = sense.getLexIndex() + 1;

			String line = String.format(SENSE_FORMAT, sensekeyLower, offset, senseNum, count);
			lines.add(line);
		}
		int n = 0;
		for (String line : lines)
		{
			ps.println(line);
			n++;
		}
		System.err.printf("Senses (lower, multikey): %d, %d lines%n", n, lines.size());
	}

	public void makeIndexLowerMultiValue(final PrintStream ps, final Map<String, Sense> sensesById)
	{
		Map<String, LinkedHashSet<Data>> entries = new TreeMap<>(String::compareToIgnoreCase);

		for (Map.Entry<String, Sense> entry : sensesById.entrySet())
		{
			// sense
			String sensekey = entry.getKey();
			Sense sense = entry.getValue();

			// offset
			String synsetId = sense.getSynsetId();
			long offset = offsets.get(synsetId);

			// sense num, tag count
			TagCount tagCount = sense.getTagCount();
			int count = tagCount == null ? 0 : tagCount.getCount();
			int senseNum = sense.getLexIndex() + 1;

			// data
			LinkedHashSet<Data> dataEntry = entries.computeIfAbsent(sensekey, s -> new LinkedHashSet<>());
			Data data = new Data(offset, senseNum, count);
			dataEntry.add(data);
		}
		int n = 0;
		for (Map.Entry<String, LinkedHashSet<Data>> dataEntry : entries.entrySet())
		{
			StringBuilder sb = new StringBuilder();
			sb.append(dataEntry.getKey().toLowerCase());
			LinkedHashSet<Data> values = dataEntry.getValue();
			List<Data> datas = new ArrayList<>(values);
			datas.sort(Comparator.comparingInt(d -> d.senseNum));
			for (Data data : datas)
			{
				sb.append(String.format(" %08d %d %d", data.offset, data.senseNum, data.tagCount));
			}
			ps.println(sb);
			n++;
		}
		System.err.printf("Senses (lower,multivalue): %d, %d lines %n", n, entries.size());
	}
}
