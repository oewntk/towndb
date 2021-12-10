/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * This class groups settings flags that affect the grinder's behaviour.
 *
 * @author Bernard Bou
 */
public class Flags
{
	/**
	 * Compat mode switch that does not allow lexid to be greater than 16. See PWN grinder source in wnparse.y
	 */
	public static boolean LEXID_COMPAT = false;

	/**
	 * Compat mode switch that does not allow pointers beyond those used in PWN.
	 */
	public static boolean POINTER_COMPAT = false;

	/**
	 * Compat mode switch that does not allow verbframes beyond those used in PWN.
	 */
	public static boolean VERBFRAME_COMPAT = false;

	/**
	 * Character set for output files.
	 */
	public static final Charset charSet = StandardCharsets.UTF_8;

	private Flags()
	{
	}
}
