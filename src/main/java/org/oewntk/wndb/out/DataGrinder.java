/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.oewntk.model.Lex;
import org.oewntk.model.Sense;
import org.oewntk.model.Synset;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * This class produces the 'data.{noun|verb|adj|adv}' files
 *
 * @author Bernard Bou
 */
public class DataGrinder extends SynsetProcessor
{
	/**
	 * Constructor
	 *
	 * @param lexesByLemma lexes mapped by lemma
	 * @param synsetsById  synsets mapped by id
	 * @param sensesById   senses mapped by id
	 * @param offsetMap    offsets by synset id
	 * @param flags        flags
	 */
	public DataGrinder(Map<String, List<Lex>> lexesByLemma, Map<String, Synset> synsetsById, Map<String, Sense> sensesById, Map<String, Long> offsetMap, int flags)
	{
		super(lexesByLemma, synsetsById, sensesById, offsetMap::get, flags);
	}

	/**
	 * Do not log things as this is the second pass, and they must already have popped up.
	 */
	@Override
	protected boolean log()
	{
		return false;
	}

	/**
	 * Make data
	 *
	 * @param ps            print stream
	 * @param synsetsById   synsets mapped by id
	 * @param posFilter     part-of-speech  filter
	 */
	public void makeData(PrintStream ps, Map<String, Synset> synsetsById, char posFilter)
	{
		ps.print(Formatter.OEWN_HEADER);
		long offset = Formatter.OEWN_HEADER.getBytes(StandardCharsets.UTF_8).length;
		Synset previous = null;

		// iterate synsets
		long n = 0;
		for (Map.Entry<String, Synset> entry : synsetsById.entrySet())
		{
			String id = entry.getKey();
			Synset synset = entry.getValue();
			if (synset.getPartOfSpeech() != posFilter)
			{
				continue;
			}
			n++;
			long offset0 = this.offsetFunction.applyAsLong(id);
			if (offset0 != offset)
			{
				assert previous != null;
				String line = getData(previous, 0);
				String line0 = new OffsetFactory(lexesByLemma, synsetsById, sensesById, flags).getData(previous, 0);
				throw new RuntimeException("miscomputed offset for " + id + "\n[then]=" + line0 + "[now ]=" + line);
			}

			String line = getData(synset, offset);
			ps.print(line);

			offset += line.getBytes(StandardCharsets.UTF_8).length;
			previous = synset;
		}
		System.err.println("Synsets: " + n + " for " + posFilter);
	}
}
