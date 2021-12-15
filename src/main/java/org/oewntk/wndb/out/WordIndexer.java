/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.oewntk.model.Lex;
import org.oewntk.model.Sense;
import org.oewntk.model.Synset;
import org.oewntk.model.TagCount;

import java.io.PrintStream;
import java.util.*;

/**
 * This class produces the 'index.{noun|verb|adj|adv}' files
 *
 * @author Bernard Bou
 */
public class WordIndexer
{
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
	 * Log print stream
	 */
	private static final PrintStream log = System.err;

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
	 * @param ps           print stream
	 * @param lexesByLemma lexes mapped by lemma
	 * @param synsetsById  synsets mapped by id
	 * @param posFilter    part-of-speech filter for lexes
	 */
	public void make(final PrintStream ps, final Map<String, List<Lex>> lexesByLemma, final Map<String, Synset> synsetsById, final char posFilter)
	{
		Map<String, Integer> incompats = new HashMap<>();

		// file header
		ps.print(Formatter.OEWN_HEADER);

		// collect entries
		Map<String, IndexEntry> indexEntries = collectIndexEntries(lexesByLemma, synsetsById, posFilter, incompats);

		// print entries
		printIndexEntries(indexEntries, ps);

		// log
		reportIncompats(incompats);
		log.printf("Words: %d for %s%n", indexEntries.size(), posFilter);
	}

	/**
	 * Collect index entries
	 *
	 * @param lexesByLemma lexes
	 * @param synsetsById  synsets
	 * @param posFilter    pos
	 * @param incompats    incompatibility log
	 * @return index entries mapped by lower-cased lemmas
	 */
	private Map<String, IndexEntry> collectIndexEntries(final Map<String, List<Lex>> lexesByLemma, final Map<String, Synset> synsetsById, final char posFilter, final Map<String, Integer> incompats)
	{
		boolean pointerCompat = (flags & Flags.pointerCompat) != 0;
		Map<String, IndexEntry> indexEntries = new TreeMap<>();

		for (Map.Entry<String, List<Lex>> entry : lexesByLemma.entrySet())
		{
			// lemma
			String lemma = entry.getKey();

			// map key
			String key = Formatter.escape(lemma.toLowerCase());

			// stream of lexes
			for (Lex lex : entry.getValue())
			{
				// filter by pos
				char pos = lex.getPartOfSpeech();
				if (pos != posFilter)
				{
					continue;
				}

				// init data mapped by lower-cased lemma
				IndexEntry indexEntry = indexEntries.computeIfAbsent(key, k -> new IndexEntry(pos));

				// senses
				List<Sense> senses = lex.getSenses();
				assert senses != null : String.format("Lex %s has no sense", lex);

				// synset ids
				collectSynsetIds(senses, indexEntry.synsetIds);

				// reindex
				if ((flags & Flags.noReIndex) == 0)
				{
					//TODO reindexSenseEntries(indexEntry.synsetIds, null);
				}

				// tag counts
				collectTagCounts(senses, indexEntry);

				// synset relations
				collectSynsetRelations(senses, synsetsById, pos, indexEntry.relationPointers, pointerCompat, incompats);

				// sense relations
				collectSenseRelations(lex, pos, indexEntry.relationPointers, pointerCompat, incompats);
			}
		}
		return indexEntries;
	}

	/**
	 * Collect synset ids
	 *
	 * @param senses    senses
	 * @param synsetIds set to collect to
	 */
	private void collectSynsetIds(final List<Sense> senses, final Set<String> synsetIds)
	{
		int previousRank = -1;
		for (Sense sense : senses)
		{
			// check ordering
			int rank = sense.getLexIndex();
			if (previousRank >= rank)
			{
				throw new IllegalArgumentException("Lex " + sense.getLex().getLemma() + " " + " previous=" + previousRank + " current=" + rank);
			}
			previousRank = rank;

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
		int previousRank = -1;
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
		Map<String, List<String>> synsetRelations = synset.getRelations();
		if (synsetRelations != null && synsetRelations.size() > 0)
		{
			for (Map.Entry<String, List<String>> relationEntry : synsetRelations.entrySet())
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
					String cause = e.getClass().getName() + ' ' + e.getMessage();
					log.printf("Illegal relation %s relid=%s%n", cause, relationType);
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
	 * @param lex           lex
	 * @param pos           pos
	 * @param pointers      set of pointers to collect to
	 * @param pointerCompat pointer compatibility flag
	 * @param incompats     incompatibility log
	 */
	private void collectSenseRelations(final Lex lex, final char pos, final Set<String> pointers, final boolean pointerCompat, final Map<String, Integer> incompats)
	{
		List<Sense> lexSenses = lex.getSenses();
		for (Sense lexSense : lexSenses)
		{
			Map<String, List<String>> senseRelations = lexSense.getRelations();
			if (senseRelations != null && senseRelations.size() > 0)
			{
				for (Map.Entry<String, List<String>> relationEntry : senseRelations.entrySet())
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
						String cause = e.getClass().getName() + ' ' + e.getMessage();
						log.printf("Illegal relation %s relid=%s%n", cause, relationType);
						continue;
					}

					// collect
					pointers.add(pointer);
				}
			}
		}
	}

	/**
	 * Print index entries
	 *
	 * @param indexEntries index entries
	 * @param ps           print stream
	 */
	private void printIndexEntries(final Map<String, IndexEntry> indexEntries, final PrintStream ps)
	{
		for (Map.Entry<String, IndexEntry> indexEntry : indexEntries.entrySet())
		{
			String key = indexEntry.getKey();
			IndexEntry data = indexEntry.getValue();
			int nSenses = data.synsetIds.size();

			String ptrs = Formatter.joinNum(data.relationPointers, "%d", String::toString);
			String ofs = Formatter.join(data.synsetIds, " ", false, s -> String.format("%08d", offsets.get(s)));
			String line = String.format(WORD_FORMAT, key, data.pos, nSenses, ptrs, nSenses, data.getTaggedSensesCount(), ofs);
			ps.println(line);
		}
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
				log.printf("Incompatibilities '%s': %d%n", entry.getKey(), entry.getValue());
			}
		}
	}
}
