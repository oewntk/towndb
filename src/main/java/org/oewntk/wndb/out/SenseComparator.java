/*
 * Copyright (c) 2022. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.oewntk.model.Sense;
import org.oewntk.model.SenseGroupings;

import java.util.Comparator;

public class SenseComparator
{
	// C O M P A R A T O R

	public static final Comparator<Sense> WNDB_SENSE_ORDER_TAIL = (s1, s2) -> {

		// sense are equal
		if (s1.equals(s2))
		{
			return 0;
		}

		// same tag count, possibly zero
		String lemma1 = s1.getLemma();
		String lemma2 = s2.getLemma();
		/*
		if (!lemma1.equalsIgnoreCase(lemma2))
		{
			throw new IllegalArgumentException(lemma1 + "-"+ lemma2);
		}
		*/

		// type a before s
		if (s1.getType() != s2.getType())
		{
			return Character.compare(s1.getType(), s2.getType());
		}

		// index in lex
		int cmpLexIndex = Integer.compare(s1.getLexIndex(), s2.getLexIndex());
		if (cmpLexIndex != 0)
		{
			// lex indexes differ
			return cmpLexIndex;
		}

		// different lemmas, upper-case first
		int cmpCase = lemma1.compareTo(lemma2);
		if (cmpCase != 0)
		{
			// different lemmas, upper-case first
			return cmpCase;
		}

		// compare sensekey
		int cmpSensekey = s1.getSensekey().compareTo(s2.getSensekey());
		if (cmpSensekey != 0)
		{
			return cmpSensekey;
		}
		throw new IllegalArgumentException(s1 + "-" + s2);
	};

	public static final Comparator<Sense> WNDB_SENSE_ORDER = SenseGroupings.BY_DECREASING_TAGCOUNT //
			.thenComparing(WN31Index.SENSE_ORDER) //
			.thenComparing(WNDB_SENSE_ORDER_TAIL);
}
