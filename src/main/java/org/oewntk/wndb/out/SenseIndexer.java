/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.oewntk.model.Sense;
import org.oewntk.model.SenseGroupings;
import org.oewntk.model.SenseGroupings.KeyLCLemmaAndPos;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class produces the 'index.sense' file
 */
public class SenseIndexer
{
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
	 * Flags
	 */
	private final int flags;

	/**
	 * Synset offsets map indexed by synsetid key
	 */
	private final Map<String, Long> offsets;

	/**
	 * Constructor
	 *
	 * @param flags   flags
	 * @param offsets synset offsets map indexed by synsetid key
	 */
	public SenseIndexer(final int flags, final Map<String, Long> offsets)
	{
		super();
		this.flags = flags;
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
		Collection<Sense> senses = sensesById.values();
		Map<KeyLCLemmaAndPos, List<Sense>> groupedSenses = SenseGroupings.sensesByLCLemmaAndPos(senses);

		/*
		groupedSenses //
				.entrySet().stream().skip(1000).limit(100) //
				.forEach(SenseIndexer::dumpSenses);
		var groupedSensesForAi = groupedSenses.get(KeyLCLemmaAndPos.of("ai", 'n'));
		SenseGroupings.dumpSensesByDecreasingTagCount(groupedSensesForAi, System.out);

		var groupedSensesForCritical = groupedSenses.get(KeyLCLemmaAndPos.of("critical", 'a'));
		SenseGroupings.dumpSensesByDecreasingTagCount(groupedSensesForCritical, System.out);

		var groupedSensesForAbsolute = groupedSenses.get(KeyLCLemmaAndPos.of("absolute", 'a'));
		SenseGroupings.dumpSensesByDecreasingTagCount(groupedSensesForAbsolute, System.out);
		*/

		// collect
		for (Entry<String, Sense> entry : sensesById.entrySet())
		{
			// sense
			String sensekey = entry.getKey();
			Sense sense = entry.getValue();

			// offset
			String synsetId = sense.getSynsetId();
			long offset = offsets.get(synsetId);

			// sense num
			int senseNum;
			if ((flags & Flags.noReIndex) == 0)
			{
				var k = KeyLCLemmaAndPos.of(sense);
				var kSenses = groupedSenses.get(k);
				kSenses.sort(SenseGroupings.byDecreasingTagCount);
				senseNum = kSenses.indexOf(sense) + 1;
			}
			else
			{
				senseNum = sense.getLexIndex() + 1;
			}

			// tag count
			int tagCount = sense.getIntTagCount();

			// print
			printSenseEntry(sensekey, offset, senseNum, tagCount, ps);
		}

		// log
		log.printf("Senses: %d%n", sensesById.size());
	}

	/**
	 * Print sense entry
	 *
	 * @param sensekey sensekey
	 * @param offset   offset
	 * @param senseNum sense number (index)
	 * @param tagCount tag count
	 * @param ps       print stream
	 */
	void printSenseEntry(String sensekey, long offset, int senseNum, int tagCount, PrintStream ps)
	{
		String line = String.format(SENSE_FORMAT, sensekey, offset, senseNum, tagCount);
		ps.println(line);
	}
}
