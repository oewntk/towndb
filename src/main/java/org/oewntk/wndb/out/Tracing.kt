/*
 * Copyright (c) 2021. Bernard Bou.
 */
package org.oewntk.wndb.out

import java.io.PrintStream

object Tracing {
	@JvmField
	val psInfo: PrintStream = System.out
	@JvmField
	val psErr: PrintStream = System.out
}
