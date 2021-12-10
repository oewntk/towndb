/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

/**
 * Format utilities
 *
 * @author Bernard Bou
 */
public class Formatter
{
	private Formatter()
	{
	}

	/**
	 * Escape string
	 *
	 * @param item string to escape
	 * @return escaped string
	 */
	static String escape(String item)
	{
		return item.replace(' ', '_');
	}

	/**
	 * Join array of items
	 *
	 * @param <T>   type of item
	 * @param items array of items of type T
	 * @param delim delimiter
	 * @return joined string representation of items
	 */
	public static <T> String join(T[] items, CharSequence delim)
	{
		if (items == null)
		{
			return "";
		}
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (T item : items)
		{
			if (first)
			{
				first = false;
			}
			else
			{
				sb.append(delim);
			}
			sb.append(item.toString());
		}
		return sb.toString();
	}

	/**
	 * Join array of ints
	 *
	 * @param items  array of ints
	 * @param delim  delimiter
	 * @param format format
	 * @return joined string representation of items
	 */
	public static String join(int[] items, CharSequence delim, String format)
	{
		if (items == null)
		{
			return "";
		}
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (int item : items)
		{
			if (first)
			{
				first = false;
			}
			else
			{
				sb.append(delim);
			}
			sb.append(String.format(format, item));
		}
		return sb.toString();
	}

	/**
	 * Join items
	 *
	 * @param <T>    type of item
	 * @param items  iteration of items of type T
	 * @param delim  delimiter
	 * @param escape whether to escape
	 * @return joined string representation of items
	 */
	public static <T> String join(Iterable<T> items, CharSequence delim, boolean escape)
	{
		return join(items, delim, escape, T::toString);
	}

	/**
	 * Join items
	 *
	 * @param <T>    type of item
	 * @param items  iteration of items of type T
	 * @param delim  delimiter
	 * @param escape whether to escape
	 * @param f      string function to represent item
	 * @return joined string representation of items
	 */
	static <T> String join(Iterable<T> items, CharSequence delim, boolean escape, Function<T, String> f)
	{
		if (items == null)
		{
			return "";
		}
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (T item : items)
		{
			if (first)
			{
				first = false;
			}
			else
			{
				sb.append(delim);
			}
			String value = f.apply(item);
			sb.append(escape ? escape(value) : value);
		}
		return sb.toString();
	}

	/**
	 * Join and quote items
	 *
	 * @param <T>    type of item
	 * @param items  array of items of type T
	 * @param delim  delimiter
	 * @param escape whether to escape
	 * @param f      string function to represent item
	 * @return joined string representation of items
	 */
	static <T> String joinAndQuote(T[] items, @SuppressWarnings("SameParameterValue") CharSequence delim, @SuppressWarnings("SameParameterValue") boolean escape, @SuppressWarnings("SameParameterValue") Function<T, String> f)
	{
		if (items == null)
		{
			return "";
		}
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (T item : items)
		{
			if (first)
			{
				first = false;
			}
			else
			{
				sb.append(delim);
			}
			String value = f == null ? item.toString() : f.apply(item);
			if (value.matches("\".*\""))
			{
				sb.append(escape ? escape(value) : value);
			}
			else
			{
				sb.append('"');
				sb.append(escape ? escape(value) : value);
				sb.append('"');
			}
		}
		return sb.toString();
	}

	/**
	 * Join items, prefix with count
	 *
	 * @param <T>         type of item
	 * @param items       collection of items of type T
	 * @param countFormat format of count field
	 * @param f           string function to represent item
	 * @return joined string representation of items preceded by count
	 */
	static <T> String joinNum(Collection<T> items, String countFormat, Function<T, String> f)
	{
		if (items == null)
		{
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(String.format(countFormat, items.size()));
		for (T item : items)
		{
			sb.append(' ');
			String value = f.apply(item);
			sb.append(value);
		}
		return sb.toString();
	}

	/**
	 * Join items in multimap
	 *
	 * @param <K>   type of key
	 * @param <V>   type of value
	 * @param map   array of items of type T
	 * @param delim delimiter
	 * @return joined string representation of items
	 */
	public static <K, V> String join(Map<K, V[]> map, CharSequence delim)
	{
		if (map == null)
		{
			return "";
		}
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Map.Entry<K, V[]> entry : map.entrySet())
		{
			if (first)
			{
				first = false;
			}
			else
			{
				sb.append(delim);
			}
			String k = entry.getKey().toString();
			String[] v = (String[]) entry.getValue();
			sb.append('[');
			sb.append(k);
			sb.append(']');
			sb.append('=');
			sb.append(join(v, ","));
		}
		return sb.toString();
	}

	/**
	 * Header of data files
	 */
	static final String PRINCETON_HEADER = //
			"  1 This software and database is being provided to you, the LICENSEE, by  \n" + //
					"  2 Princeton University under the following license.  By obtaining, using  \n" + //
					"  3 and/or copying this software and database, you agree that you have  \n" + //
					"  4 read, understood, and will comply with these terms and conditions.:  \n" + //
					"  5   \n" + //
					"  6 Permission to use, copy, modify and distribute this software and  \n" + //
					"  7 database and its documentation for any purpose and without fee or  \n" + //
					"  8 royalty is hereby granted, provided that you agree to comply with  \n" + //
					"  9 the following copyright notice and statements, including the disclaimer,  \n" + //
					"  10 and that the same appear on ALL copies of the software, database and  \n" + //
					"  11 documentation, including modifications that you make for internal  \n" + //
					"  12 use or for distribution.  \n" + //
					"  13   \n" + //
					"  14 WordNet 3.1 Copyright 2011 by Princeton University.  All rights reserved.  \n" + //
					"  15   \n" + //
					"  16 THIS SOFTWARE AND DATABASE IS PROVIDED \"AS IS\" AND PRINCETON  \n" + //
					"  17 UNIVERSITY MAKES NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR  \n" + //
					"  18 IMPLIED.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, PRINCETON  \n" + //
					"  19 UNIVERSITY MAKES NO REPRESENTATIONS OR WARRANTIES OF MERCHANT-  \n" + //
					"  20 ABILITY OR FITNESS FOR ANY PARTICULAR PURPOSE OR THAT THE USE  \n" + //
					"  21 OF THE LICENSED SOFTWARE, DATABASE OR DOCUMENTATION WILL NOT  \n" + //
					"  22 INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR  \n" + //
					"  23 OTHER RIGHTS.  \n" + //
					"  24   \n" + //
					"  25 The name of Princeton University or Princeton may not be used in  \n" + //
					"  26 advertising or publicity pertaining to distribution of the software  \n" + //
					"  27 and/or database.  Title to copyright in this software, database and  \n" + //
					"  28 any associated documentation shall at all times remain with  \n" + //
					"  29 Princeton University and LICENSEE agrees to preserve same.  \n"; //

	static final String OEWN_HEADER = //
			"  1 This software and database is being provided to you, the LICENSEE, by  \n" + //
					"  2 the Open English Wordnet team under the Creative Commons Attribution 4.0  \n" + //
					"  3 International License (CC-BY 4.0).  \n" + //
					"  4 Open English Wordnet 2021 Copyright 2021 by the Open English Wordnet team.  \n" + //
					"  5 \n" + //
					"  6 Permission to use, copy, modify and distribute this software and  \n" + //
					"  7 database and its documentation for any purpose and without fee or  \n" + //
					"  8 royalty is hereby granted, provided that you agree to comply with  \n" + //
					"  9 the following copyright notice and statements, including the disclaimer,  \n" + //
					"  10 and that the same appear on ALL copies of the software, database and  \n" + //
					"  11 documentation, including modifications that you make for internal  \n" + //
					"  12 use or for distribution.  \n" + //
					"  13 \n" + //
					"  14 WordNet 3.1 Copyright 2011 by Princeton University.  All rights reserved.  \n" + //
					"  15 THIS SOFTWARE AND DATABASE IS PROVIDED \"AS IS\" AND PRINCETON  \n" + //
					"  16 UNIVERSITY MAKES NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR  \n" + //
					"  17 IMPLIED.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, PRINCETON  \n" + //
					"  18 UNIVERSITY MAKES NO REPRESENTATIONS OR WARRANTIES OF MERCHANT-  \n" + //
					"  19 ABILITY OR FITNESS FOR ANY PARTICULAR PURPOSE OR THAT THE USE  \n" + //
					"  20 OF THE LICENSED SOFTWARE, DATABASE OR DOCUMENTATION WILL NOT  \n" + //
					"  21 INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR  \n" + //
					"  22 OTHER RIGHTS.  \n" + //
					"  23 The name of Princeton University or Princeton may not be used in  \n" + //
					"  24 advertising or publicity pertaining to distribution of the software  \n" + //
					"  25 and/or database.  Title to copyright in this software, database and  \n" + //
					"  26 any associated documentation shall at all times remain with  \n" + //
					"  27 Princeton University and LICENSEE agrees to preserve same.  \n" + //
					"  28 \n" + //
					"  29 Ground by x-englishwordnet     \n";

	static public void main(String[] args)
	{
		System.out.println(PRINCETON_HEADER.length());
		System.out.println(OEWN_HEADER.length());
	}
}