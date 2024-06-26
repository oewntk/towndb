/*
 * Copyright (c) 2021-2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.CoreModel
import org.oewntk.model.Model
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.util.function.Consumer

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
    private val ps: PrintStream,
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
        ps.println("[CoreModel] map ${model.source}")

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
                offsets.keys
                    .asSequence()
                    .sorted()
                    .forEach { ps.println("$it ${offsets[it]}") }
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
            return file.useLines { lines ->
                lines
                    .map { it.split("\\s".toRegex()).dropLastWhile { it2 -> it2.isEmpty() }.toTypedArray() }
                    .map { it[0] to it[1].toLong() }
                    .toMap()
            }
        }
    }
}
