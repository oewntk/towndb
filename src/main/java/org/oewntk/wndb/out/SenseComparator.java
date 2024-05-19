/*
 * Copyright (c) 2022. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.oewntk.model.Sense;
import org.oewntk.model.SenseGroupings;

import java.util.Comparator;

/**
 * Sense comparators
 */
public class SenseComparator
{
	/**
	 * Tail comparator, after head comparators (tagcount) fail
	 */
	public static final Comparator<Sense> WNDB_SENSE_ORDER_TAIL = (s1, s2) -> {

		// sense are equal
		if (s1.equals(s2))
		{
			return 0;
		}

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

		String lemma1 = s1.getLemma();
		String lemma2 = s2.getLemma();

		// lemmas, regardless of case
		int cmpLowerCase = lemma1.compareToIgnoreCase(lemma2);
		if (cmpLowerCase != 0)
		{
			// different lemmas, regardless of case
			return cmpLowerCase;
		}

		// lemmas, cased
		int cmpCase = lemma1.compareTo(lemma2);
		if (cmpCase != 0)
		{
			// different cased lemmas, upper-case last
			return -cmpCase;
		}

		// compare sensekey
		int cmpSensekey = s1.getSensekey().compareTo(s2.getSensekey());
		if (cmpSensekey != 0)
		{
			return cmpSensekey;
		}
		throw new IllegalArgumentException(s1 + "-" + s2);
	};

	/**
	 * Define a total order of senses
	 */
	public static final Comparator<Sense> WNDB_SENSE_ORDER = SenseGroupings.BY_DECREASING_TAGCOUNT //
			.thenComparing(WN31Index.SENSE_ORDER) //
			.thenComparing(WNDB_SENSE_ORDER_TAIL);
}
