/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

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
	public static final int lexIdCompat = 0x1;

	/**
	 * Compat mode switch that does not allow pointers beyond those used in PWN.
	 */
	public static final int pointerCompat = 0x2;

	/**
	 * Compat mode switch that does not allow verbframes beyond those used in PWN.
	 */
	public static final int verbFrameCompat = 0x4;
}
