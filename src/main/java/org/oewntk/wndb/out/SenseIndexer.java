/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.oewntk.model.Sense;
import org.oewntk.model.TagCount;

import java.io.PrintStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

/**
 * This class produces the 'index.sense' file
 */
public class SenseIndexer
{
	/**
	 * This represents what is needed for a line in index.sense)
	 */
	private static class SenseEntry
	{
		public final long offset;

		public final int senseNum;

		public final int tagCount;

		public SenseEntry(long offset, int senseNum, int tagCount)
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
			SenseEntry senseEntry = (SenseEntry) o;
			return offset == senseEntry.offset && senseNum == senseEntry.senseNum && tagCount == senseEntry.tagCount;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(offset, senseNum, tagCount);
		}
	}

	/**
	 * Log print stream
	 */
	private static final PrintStream log = System.err;

	/**
	 * Format in data file
	 * sense_key
	 * synset_offset
	 * sense_number
	 * tag_cnt
	 */
	private static final String SENSE_FORMAT = "%s %08d %d %d";

	/**
	 * Synset offsets map indexed by synsetid key
	 */
	private final Map<String, Long> offsets;

	/**
	 * Constructor
	 *
	 * @param offsets synset offsets map indexed by synsetid key
	 */
	public SenseIndexer(final Map<String, Long> offsets)
	{
		super();
		this.offsets = offsets;
	}

	/**
	 * Make senses
	 *
	 * @param ps         print stream
	 * @param sensesById senses
	 */
	public void make(final PrintStream ps, final Map<String, Sense> sensesById)
	{
		// collect
		Map<String, SenseEntry> entries = collectSenseEntries(sensesById);

		// print
		printSenseEntries(entries, ps);

		// log
		log.printf("Senses: %d%n", entries.size());
	}

	/**
	 * Collect sense entries
	 *
	 * @param sensesById senses
	 * @return sense entries mapped by sensekey
	 */
	private Map<String, SenseEntry> collectSenseEntries(final Map<String, Sense> sensesById)
	{
		Map<String, SenseEntry> entries = new TreeMap<>(String::compareToIgnoreCase);

		for (Entry<String, Sense> entry : sensesById.entrySet())
		{
			// sense
			String sensekey = entry.getKey();
			Sense sense = entry.getValue();

			// offset
			String synsetId = sense.getSynsetId();
			long offset = offsets.get(synsetId);

			// sense num
			int senseNum = sense.getLexIndex() + 1;

			// tag count
			TagCount tagCount = sense.getTagCount();
			int tagCountValue = tagCount == null ? 0 : tagCount.getCount();

			// collect
			entries.put(sensekey, new SenseEntry(offset, senseNum, tagCountValue));
		}
		return entries;
	}

	/**
	 * Print sense entries
	 *
	 * @param entries sense entries
	 * @param ps      print stream
	 */
	void printSenseEntries(Map<String, SenseEntry> entries, PrintStream ps)
	{
		for (Entry<String, SenseEntry> dataEntry : entries.entrySet())
		{
			String key = dataEntry.getKey();
			SenseEntry senseEntry = dataEntry.getValue();
			String line = String.format(SENSE_FORMAT, key, senseEntry.offset, senseEntry.senseNum, senseEntry.tagCount);
			ps.println(line);
		}
	}
}
