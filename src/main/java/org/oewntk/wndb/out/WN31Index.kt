/*
 * Copyright (c) 2022. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.Sense
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object WN31Index {
	/**
	 * Resource file
	 */
	private const val RES = "/index.sense.31"

	/**
	 * Sensekey to index map
	 */
	private val SK2INDEX = readIndexes()

	/**
	 * Sensekey comparator based on the sensekey-to-index map
	 */
	private val WN31_SK_COMPARATOR: Comparator<String> = Comparator.comparing { SK2INDEX[it]!! }

	/**
	 * Sense comparator based on the sensekey-to-index map
	 */
	private val WN31_SENSE_ORDER: Comparator<Sense> = Comparator.comparing(Sense::senseKey, WN31_SK_COMPARATOR)

	/**
	 * Sense comparator based on the 3.1 sensekey-to-index map. This comparator does not define a total order. It returns 0 when the sensekeys are not defined in 3.1
	 */
	val SENSE_ORDER: Comparator<Sense> = Comparator { s1: Sense, s2: Sense ->
		val i1 = SK2INDEX[s1.senseKey]
		val i2 = SK2INDEX[s2.senseKey]
		if (i1 == null || i2 == null) {
			return@Comparator 0 // fail, to be chained with thenCompare
		}
		val cmp = i1.compareTo(i2)
		assert(cmp != 0) { String.format("Senses have equal indexes %s %s", s1, s2) }
		cmp
	}

	/**
	 * Read indexes from resource index.sense.31, a stripped-down version of index sense with three columns
	 * sensekey index tagcount
	 *
	 * @return sensekey-to-index map
	 */
	private fun readIndexes(): Map<String, Int> {

		val map: MutableMap<String, Int> = HashMap()

		val url = checkNotNull(WN31Index::class.java.getResource(RES))
		try {
			BufferedReader(InputStreamReader(url.openStream(), StandardCharsets.UTF_8)).use { it ->
				var line: String
				while ((it.readLine().also { line = it }) != null) {
					if (line.isEmpty()) {
						continue
					}
					try {
						val fields = line.split("\\s".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
						val field1 = fields[0]
						val field2 = fields[1].toInt()
						map[field1] = field2
					} catch (e: RuntimeException) {
						Tracing.psErr.println("[E] reading at line '$line' $e")
					}
				}
			}
		} catch (e: IOException) {
			Tracing.psErr.println("[E] reading WN31 index $e")
		}
		return map
	}

	/**
	 * Main
	 *
	 * @param ignoredArgs command-line arguments
	 */
	@JvmStatic
	fun main(ignoredArgs: Array<String>) {
		val list = listOf("eight%1:06:00::", "eight%1:14:00::", "eight%1:23:00::")
		list
			.sortedWith(WN31_SK_COMPARATOR)
			.forEach { println(it) }
	}
}
