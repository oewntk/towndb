/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.oewntk.model.Sense;
import org.oewntk.model.SenseGroupings;
import org.oewntk.model.Synset;
import org.oewntk.model.TagCount;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class produces the 'index.{noun|verb|adj|adv}' files
 *
 * @author Bernard Bou
 */
public class WordIndexer
{
	private static final boolean LOG = false;

	/**
	 * This represents what is needed for a line in index.(noun|verb|adj|adv)
	 */
	private static class IndexEntry
	{
		final char pos;

		private int taggedSensesCount = 0;

		final Set<String> synsetIds = new LinkedHashSet<>();

		final Set<String> relationPointers = new TreeSet<>();

		public IndexEntry(final char pos)
		{
			this.pos = pos;
		}

		public void incTaggedCount()
		{
			taggedSensesCount += 1;
		}

		public int getTaggedSensesCount()
		{
			return taggedSensesCount;
		}
	}

	/**
	 * Format in data file
	 * lemma
	 * pos
	 * synset_cnt
	 * ptrs=p_cnt  [ptr_symbol...]
	 * ofs=sense_cnt
	 * tag sense cnt (number of senses that are tagged)
	 * synset_offset  [synset_offset...]
	 */
	private static final String WORD_FORMAT = "%s %s %d %s %d %d %s  ";

	/**
	 * Synset offsets indexed by synset id key
	 */
	private final Map<String, Long> offsets;

	/**
	 * Flags
	 */
	private final int flags;

	/**
	 * Constructor
	 *
	 * @param offsets offsets indexed by synset id key
	 * @param flags   flags
	 */
	public WordIndexer(final Map<String, Long> offsets, final int flags)
	{
		this.offsets = offsets;
		this.flags = flags;
	}

	/**
	 * Make index
	 *
	 * @param ps          print stream
	 * @param senses      senses
	 * @param synsetsById synsets mapped by id
	 * @param posFilter   part-of-speech filter for lexes
	 * @return number of indexes
	 */
	public long make(final PrintStream ps, final Collection<Sense> senses, final Map<String, Synset> synsetsById, final char posFilter)
	{
		Map<String, Integer> incompats = new HashMap<>();

		// file header
		ps.print(Formatter.OEWN_HEADER);

		// collect entries
		boolean pointerCompat = (flags & Flags.pointerCompat) != 0;
		Map<String, IndexEntry> indexEntries = new TreeMap<>();

		var groupedSenses = SenseGroupings.sensesByLCLemmaAndPos(senses);
		groupedSenses.entrySet().stream() //
				.filter(e -> e.getKey().pos == posFilter) //
				.sorted(Comparator.comparing(s -> s.getKey().lcLemma.replace(' ', '_'))) //
				.forEach(e -> {

					var k = e.getKey();
					var kSenses = e.getValue().stream().sorted(SenseComparator.WNDB_SENSE_ORDER).collect(Collectors.toList());

					var lcLemma = k.lcLemma;
					var pos = k.pos;
					var ik = lcLemma.replace(' ', '_');
					var eik = Formatter.escape(ik);

					// init data mapped by lower-cased lemma
					IndexEntry indexEntry = indexEntries.computeIfAbsent(eik, ke -> new IndexEntry(pos));

					// synset ids
					collectSynsetIds(kSenses, indexEntry.synsetIds);

					// tag counts
					collectTagCounts(kSenses, indexEntry);

					// synset relations
					collectSynsetRelations(kSenses, synsetsById, pos, indexEntry.relationPointers, pointerCompat, incompats);

					// sense relations
					collectSenseRelations(kSenses, pos, indexEntry.relationPointers, pointerCompat, incompats);

					// print
					printIndexEntry(eik, indexEntry, ps);
				});

		// log
		reportIncompats(incompats);
		//Tracing.psInfo.printf("Words: %d for %s%n", indexEntries.size(), posFilter);
		return indexEntries.size();
	}

	/**
	 * Collect synset ids
	 *
	 * @param senses    senses
	 * @param synsetIds set to collect to
	 */
	private void collectSynsetIds(final List<Sense> senses, final Set<String> synsetIds)
	{
		// int previousRank = -1;
		for (Sense sense : senses)
		{
			/*
			// check ordering
			int rank = sense.getLexIndex();
			if (previousRank >= rank)
			{
				throw new IllegalArgumentException("Lex " + sense.getLex().getLemma() + " " + " previous=" + previousRank + " current=" + rank);
			}
			previousRank = rank;
			*/

			// synsetid
			String synsetId = sense.getSynsetId();

			// collect
			synsetIds.add(synsetId);
		}
	}

	/**
	 * Collect synset ids
	 *
	 * @param senses     senses
	 * @param indexEntry to collect to
	 */
	private void collectTagCounts(final List<Sense> senses, final IndexEntry indexEntry)
	{
		for (Sense sense : senses)
		{
			// synsetid
			TagCount tagCount = sense.getTagCount();
			if (tagCount == null)
			{
				continue;
			}

			// collect
			if (tagCount.getCount() > 0)
			{
				indexEntry.incTaggedCount();
			}
		}
	}

	/**
	 * Collect synset relations
	 *
	 * @param senses        senses
	 * @param synsetsById   synsets by id
	 * @param pos           pos
	 * @param pointers      set of pointers to collect to
	 * @param pointerCompat pointer compatibility flag
	 * @param incompats     incompatibility log
	 */
	private void collectSynsetRelations(final List<Sense> senses, final Map<String, Synset> synsetsById, final char pos, final Set<String> pointers, final boolean pointerCompat, final Map<String, Integer> incompats)
	{
		for (Sense sense : senses)
		{
			// synsetid
			String synsetId = sense.getSynsetId();

			// synset relations
			Synset synset = synsetsById.get(synsetId);
			collectSynsetRelations(synset, pos, pointers, pointerCompat, incompats);
		}
	}

	/**
	 * Collect sense relations
	 *
	 * @param synset        synset
	 * @param pos           pos
	 * @param pointers      set of pointers to collect to
	 * @param pointerCompat pointer compatibility flag
	 * @param incompats     incompatibility log
	 */
	private void collectSynsetRelations(final Synset synset, final char pos, final Set<String> pointers, final boolean pointerCompat, final Map<String, Integer> incompats)
	{
		Map<String, Set<String>> synsetRelations = synset.getRelations();
		if (synsetRelations != null && synsetRelations.size() > 0)
		{
			for (Map.Entry<String, Set<String>> relationEntry : synsetRelations.entrySet())
			{
				String relationType = relationEntry.getKey();
				String pointer;
				try
				{
					pointer = Coder.codeRelation(relationType, pos, pointerCompat);
				}
				catch (CompatException e)
				{
					String cause = e.getCause().getMessage();
					int count = incompats.computeIfAbsent(cause, (c) -> 0) + 1;
					incompats.put(cause, count);
					continue;
				}
				catch (IllegalArgumentException e)
				{
					//String cause = e.getClass().getName() + ' ' + e.getMessage();
					if (LOG)
					{
						Tracing.psErr.printf("[W] Discarded relation '%s'%n", relationType);
					}
					throw e;
				}

				// collect
				pointers.add(pointer);
			}
		}
	}

	/**
	 * Collect sense relations
	 *
	 * @param senses        senses
	 * @param pos           pos
	 * @param pointers      set of pointers to collect to
	 * @param pointerCompat pointer compatibility flag
	 * @param incompats     incompatibility log
	 */
	private void collectSenseRelations(final List<Sense> senses, final char pos, final Set<String> pointers, final boolean pointerCompat, final Map<String, Integer> incompats)
	{
		for (Sense lexSense : senses)
		{
			Map<String, Set<String>> senseRelations = lexSense.getRelations();
			if (senseRelations != null && senseRelations.size() > 0)
			{
				for (Map.Entry<String, Set<String>> relationEntry : senseRelations.entrySet())
				{
					String relationType = relationEntry.getKey();
					String pointer;
					try
					{
						pointer = Coder.codeRelation(relationType, pos, pointerCompat);
					}
					catch (CompatException e)
					{
						String cause = e.getCause().getMessage();
						int count = incompats.computeIfAbsent(cause, (c) -> 0) + 1;
						incompats.put(cause, count);
						continue;
					}
					catch (IllegalArgumentException e)
					{
						//String cause = e.getClass().getName() + ' ' + e.getMessage();
						if (LOG)
						{
							Tracing.psErr.printf("[W] Discarded relation '%s'%n", relationType);
						}
						continue;
					}

					// collect
					pointers.add(pointer);
				}
			}
		}
	}

	/**
	 * Print index entry
	 *
	 * @param key        index key
	 * @param indexEntry index data
	 * @param ps         print stream
	 */
	private void printIndexEntry(final String key, final IndexEntry indexEntry, final PrintStream ps)
	{
		int nSenses = indexEntry.synsetIds.size();
		String ptrs = Formatter.joinNum(indexEntry.relationPointers, "%d", String::toString);
		String ofs = Formatter.join(indexEntry.synsetIds, " ", false, s -> String.format("%08d", offsets.get(s)));
		String line = String.format(WORD_FORMAT, key, indexEntry.pos, nSenses, ptrs, nSenses, indexEntry.getTaggedSensesCount(), ofs);
		ps.println(line);
	}

	/**
	 * Report incompatibilities to log
	 *
	 * @param incompats incompatibilities
	 */
	private void reportIncompats(final Map<String, Integer> incompats)
	{
		if (incompats.size() > 0)
		{
			for (Map.Entry<String, Integer> entry : incompats.entrySet())
			{
				Tracing.psErr.printf("[W] Incompatibility '%s': %d%n", entry.getKey(), entry.getValue());
			}
		}
	}
}
