/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.oewntk.model.Sense;
import org.oewntk.model.SenseGroupings;
import org.oewntk.model.SenseGroupings.KeyLCLemmaAndPos;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * This class produces the 'index.sense' file
 */
public class SenseIndexer
{
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
		var groupedSenses = SenseGroupings.sensesByLCLemmaAndPos(senses);

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
				// this will yield diverging sensenums to senses varying only in lemma, not synsetid target, e.g. a%1:10:00:: and a%1:10:01::
				// var kSenses = groupedSenses.get(k).stream().sorted(SenseGroupings.byDecreasingTagCount).collect(Collectors.toList());
				// senseNum = kSenses.indexOf(sense) + 1;
				// the following will yield the same sensenum to senses varying only in lemma, not synsetid target, e.g. a%1:10:00:: and a%1:10:01::
				var kTargetSynsetIds = groupedSenses.get(k).stream().sorted(SenseGroupings.byDecreasingTagCount).map(Sense::getSynsetId).distinct().collect(Collectors.toList());
				senseNum = kTargetSynsetIds.indexOf(sense.getSynsetId()) + 1;
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
		Tracing.psInfo.printf("Senses: %d%n", sensesById.size());
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
