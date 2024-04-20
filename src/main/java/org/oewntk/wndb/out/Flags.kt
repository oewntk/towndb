/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */
package org.oewntk.wndb.out

/**
 * This class groups settings flags that affect the grinder's behaviour.
 *
 * @author Bernard Bou
 */
object Flags {
	/**
	 * Compat mode switch that does not allow lexid to be greater than 16. See PWN grinder source in wnparse.y
	 */
	const val lexIdCompat: Int = 0x1

	/**
	 * Compat mode switch that does not allow pointers beyond those used in PWN.
	 */
	const val pointerCompat: Int = 0x2

	/**
	 * Compat mode switch that does not allow verbframes beyond those used in PWN.
	 */
	const val verbFrameCompat: Int = 0x4

	/**
	 * Reindex indexes
	 */
	const val noReIndex: Int = 0x10000000
}
