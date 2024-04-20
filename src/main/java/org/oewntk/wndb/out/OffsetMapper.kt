/*
 * Copyright (c) 2021-2021. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.CoreModel
import org.oewntk.model.Model
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.function.Consumer
import java.util.stream.Collectors

/**
 * Main class that generates the serialized synsetId to offset map
 *
 * @param outDir output directory
 * @param flags  flags
 * @param ps     log print stream
 *
 * @author Bernard Bou
 */
class OffsetMapper(
	private val outDir: File,
	private val flags: Int,
	private val ps: PrintStream
) : Consumer<Model> {

	override fun accept(model: Model) {
		try {
			grind(model)
		} catch (e: IOException) {
			e.printStackTrace(Tracing.psErr)
		}
	}

	/**
	 * Grind
	 *
	 * @param model model
	 * @throws IOException io exception
	 */
	@Throws(IOException::class)
	fun grind(model: CoreModel) {
		// Model
		ps.printf("[CoreModel] %s%n", model.source)

		// Output
		if (!outDir.exists()) {
			outDir.mkdirs()
		}

		// Compute synset offsets
		val offsets = GrindOffsets(model.lexesByLemma!!, model.synsetsById!!, model.sensesById!!, flags).compute()

		// Serialize offsets
		writeOffsets(offsets, File(outDir, FILE_OFFSET_MAP))
	}

	companion object {
		const val FILE_OFFSET_MAP: String = "offsets.map"

		/**
		 * Write offsets to file
		 *
		 * @param offsets offsets by synset id
		 * @param file    out file
		 * @throws IOException io exception
		 */
		@Throws(IOException::class)
		fun writeOffsets(offsets: Map<String, Long>, file: File) {
			PrintStream(FileOutputStream(file), true, StandardCharsets.UTF_8).use { ps ->
				offsets.keys.stream() //
					.sorted() //
					.forEach { k: String? -> ps.printf("%s %d%n", k, offsets[k]) }
			}
		}

		/**
		 * Read offsets from file
		 *
		 * @param file in file
		 * @return offsets by synset id
		 * @throws IOException io exception
		 */
		@Throws(IOException::class)
		fun readOffsets(file: File): Map<String, Long> {
			Files.lines(file.toPath()).use { s ->
				return s //
					.map { it.split("\\s".toRegex()).dropLastWhile { it2 -> it2.isEmpty() }.toTypedArray() }
					.collect(Collectors.toMap(
						{ it[0] },
						{ it[1].toLong() }))
			}
		}
	}
}
