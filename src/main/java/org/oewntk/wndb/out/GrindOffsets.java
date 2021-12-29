/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.oewntk.model.Lex;
import org.oewntk.model.Sense;
import org.oewntk.model.Synset;
import org.oewntk.wndb.out.Data.Relation;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * This class computes file offsets that serve as synset id in the WNDB format. It does so by iterating over synsets and yielding a dummy line string of
 * the same length as the final string. The offset counter is moved by the line's length.
 *
 * @author Bernard Bou
 */
public class GrindOffsets extends SynsetProcessor
{
	/**
	 * Constructor
	 *
	 * @param lexesByLemma lexes mapped by lemma
	 * @param synsetsById  synsets mapped by id
	 * @param sensesById   senses mapped by id
	 * @param flags        flags
	 */
	public GrindOffsets(Map<String, Collection<Lex>> lexesByLemma, Map<String, Synset> synsetsById, Map<String, Sense> sensesById, int flags)
	{
		super(lexesByLemma, synsetsById, sensesById, s -> 0L /* dummy synset */, flags);
	}

	/**
	 * Log things on the first pass
	 */
	@Override
	protected boolean log()
	{
		return false;
	}

	/**
	 * Compute synset offsets
	 *
	 * @param posFilter     selection of synsets
	 * @param offsets       result map
	 */
	public void compute(char posFilter, Map<String, Long> offsets)
	{
		long offset = Formatter.OEWN_HEADER.getBytes(StandardCharsets.UTF_8).length;

		// iterate synsets
		for (Map.Entry<String, Synset> entry : synsetsById.entrySet())
		{
			String id = entry.getKey();
			Synset synset = entry.getValue();
			if (synset.getPartOfSpeech() != posFilter)
			{
				continue;
			}

			String data = getData(synset, dummyOfs);
			offsets.put(id, offset);

			offset += data.getBytes(StandardCharsets.UTF_8).length;
		}
		//Tracing.psInfo.println("Computed offsets for " + posFilter);
	}

	/**
	 * Compute offsets mapped by synsetId
	 *
	 * @return map of offsets by synsetId
	 */
	public Map<String, Long> compute()
	{
		Map<String, Long> offsets = new HashMap<>();
		compute(Data.NOUN_POS_FILTER, offsets);
		compute(Data.VERB_POS_FILTER, offsets);
		compute(Data.ADJ_POS_FILTER, offsets);
		compute(Data.ADV_POS_FILTER, offsets);
		return offsets;
	}

	// I M P L E M E N T A T I O N

	private final long dummyOfs = this.offsetFunction.applyAsLong("");

	private static final int DUMMY_NUM = 0;

	@Override
	protected Relation buildSenseRelation(String type, char pos, int sourceMemberNum, Sense targetSense, Synset targetSynset, String targetSynsetId) throws CompatException
	{
		char targetType = targetSynset.getType();
		boolean pointerCompat = (flags & Flags.pointerCompat) != 0;
		return new Relation(type, pos, targetType, dummyOfs, DUMMY_NUM, DUMMY_NUM, pointerCompat);
	}

	@Override
	protected int buildLexfileNum(Synset synset)
	{
		return DUMMY_NUM;
	}
}
