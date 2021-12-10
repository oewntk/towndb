/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.oewntk.model.Lex;
import org.oewntk.model.Sense;
import org.oewntk.model.Synset;

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
	 * Format in data file
	 */
	private static final String WORD_FORMAT = "%s %s %d %s %d %d %s  ";
	// lemma
	// pos
	// synset_cnt
	// ptrs=p_cnt  [ptr_symbol...]
	// ofs=sense_cnt
	// tag sense cnt (0)
	// synset_offset  [synset_offset...]

	/**
	 * Synset offsets indexed by synset id key
	 */
	private final Map<String, Long> offsets;

	/**
	 * Constructor
	 *
	 * @param offsets offsets indexed by synset id key
	 */
	public WordIndexer(Map<String, Long> offsets)
	{
		this.offsets = offsets;
	}

	private static class IndexData
	{
		private char pos;

		final Set<String> synsetIds = new LinkedHashSet<>();

		final Set<String> relationPointers = new TreeSet<>();

		public char getPartOfSpeech()
		{
			return pos;
		}
	}

	/**
	 * Make index
	 *
	 * @param ps           print stream
	 * @param lexesByLemma lexes mapped by lemma
	 * @param synsetsById  synsets mapped by id
	 * @param posFilter    part-of-speech filter for lexes
	 */
	public void makeIndex(PrintStream ps, Map<String, List<Lex>> lexesByLemma, Map<String, Synset> synsetsById, char posFilter)
	{
		Map<String, Integer> incompats = new HashMap<>();

		ps.print(Formatter.OEWN_HEADER);

		// collect lines in a set to avoid duplicate lines that arise from lower casing of lemma
		Map<String, IndexData> indexEntries = new TreeMap<>();

		int n = 0;
		for (Map.Entry<String, List<Lex>> entry : lexesByLemma.entrySet())
		{
			String lemma = entry.getKey();
			for (Lex lex : entry.getValue())
			{
				if (lex.getPartOfSpeech() != posFilter)
				{
					continue;
				}

				// lemma, pos
				String key = Formatter.escape(lemma.toLowerCase());
				char pos = lex.getPartOfSpeech();

				// init
				IndexData data = indexEntries.computeIfAbsent(key, k -> new IndexData());

				// pos
				data.pos = pos;

				// senses
				List<Sense> senses = lex.getSenses();
				if (senses == null)
				{
					throw new IllegalArgumentException("LexicalEntry " + lex.getLemma() + " has no Sense");
				}
				else
				{
					int previousRank = -1;
					for (Sense sense : senses)
					{
						// check ordering
						int rank = sense.getLexIndex();
						if (previousRank >= rank)
						{
							throw new IllegalArgumentException("LexEntry " + lex.getLemma() + " " + " previous=" + previousRank + " current=" + rank);
						}
						previousRank = rank;

						// synsetid
						String synsetId = sense.getSynsetId();
						data.synsetIds.add(synsetId);

						// target synset
						Synset synset = synsetsById.get(synsetId);

						// synset relations
						Map<String, List<String>> synsetRelations = synset.getRelations();
						if (synsetRelations != null && synsetRelations.size() > 0)
						{
							for (Map.Entry<String, List<String>> relationEntry : synsetRelations.entrySet())
							{
								String relationType = relationEntry.getKey();
								String pointer;
								try
								{
									pointer = Coder.codeRelation(relationType, pos);
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
									System.err.printf("Illegal relation %s relid=%s%n", cause, relationType);
									throw e;
								}
								data.relationPointers.add(pointer);
							}
						}
					}
				}

				// sense relations
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
								pointer = Coder.codeRelation(relationType, pos);
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
								System.err.printf("Illegal relation %s relid=%s%n", cause, relationType);
								continue;
							}
							data.relationPointers.add(pointer);
						}
					}
				}
				n++;
			}
		}
		int count = 0;
		for (Map.Entry<String, IndexData> indexEntry : indexEntries.entrySet())
		{
			String key = indexEntry.getKey();
			IndexData data = indexEntry.getValue();
			int nSenses = data.synsetIds.size();

			String ptrs = Formatter.joinNum(data.relationPointers, "%d", String::toString);
			String ofs = Formatter.join(data.synsetIds, " ", false, s -> String.format("%08d", offsets.get(s)));
			String line = String.format(WORD_FORMAT, key, data.getPartOfSpeech(), nSenses, ptrs, nSenses, 0, ofs);
			ps.println(line);
			count++;
		}

		// report incompats
		if (incompats.size() > 0)
		{
			for (Map.Entry<String, Integer> entry : incompats.entrySet())
			{
				System.err.printf("Incompatibilities '%s': %d%n", entry.getKey(), entry.getValue());
			}
		}
		System.err.println("Words: " + count + '/' + n + " lexes for " + posFilter);
	}
}
