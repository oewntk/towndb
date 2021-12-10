/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.oewntk.model.Lex;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class produces the index.{noun|verb|adj|adv}.exc files
 *
 * @author Bernard Bou
 */
public class MorphGrinder
{
	/**
	 * Constructor
	 */
	public MorphGrinder()
	{
	}

	/**
	 * Make morph files
	 *
	 * @param ps           print stream
	 * @param lexesByLemma lexes mapped by lemma
	 * @param posFilter    filter selecting lexes
	 */
	public void makeMorph(PrintStream ps, Map<String, List<Lex>> lexesByLemma, char posFilter)
	{
		Set<String> lines = new TreeSet<>();

		// iterate lexes
		int n = 0;
		for (Map.Entry<String, List<Lex>> entry : lexesByLemma.entrySet())
		{
			String lemma = entry.getKey();
			List<Lex> lexes = entry.getValue();
			for (Lex lex : lexes)
			{
				if (lex.getPartOfSpeech() != posFilter)
				{
					continue;
				}

				String[] forms = lex.getForms();
				if (forms != null)
				{
					for (String form : forms)
					{
						String line = String.format("%s %s", form, lemma);
						lines.add(line);
						n++;
					}
				}
			}
		}
		for (String line : lines)
		{
			ps.println(line);
		}
		System.err.println("Morphs: " + n + " for " + posFilter);
	}
}
