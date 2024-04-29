/*
 * Copyright (c) 2021-2021. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.CoreModel
import org.oewntk.model.Model
import java.io.*
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
class OffsetSerializer(
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
        ps.printf("[CoreModel] %s%n", model.source)

        // Output
        if (!outDir.exists()) {
            outDir.mkdirs()
        }

        // Compute synset offsets
        val offsets = GrindOffsets(model.lexesByLemma!!, model.synsetsById!!, model.sensesById!!, flags).compute()

        // Serialize offsets
        serializeOffsets(offsets, File(outDir, FILE_OFFSET_MAP))
    }

    companion object {

        const val FILE_OFFSET_MAP: String = "offsets.ser"

        /**
         * Serialize offsets
         *
         * @param offsets synset_id-to-offset map
         * @param file    out file
         * @throws IOException io exception
         */
        @Throws(IOException::class)
        fun serializeOffsets(offsets: Map<String, Long>, file: File) {
            FileOutputStream(file).use { os ->
                serialize(os, offsets)
            }
        }

        /**
         * Serialize object
         *
         * @param os     output stream
         * @param object object
         * @throws IOException io exception
         */
        @Throws(IOException::class)
        private fun serialize(os: OutputStream, `object`: Any) {
            ObjectOutputStream(os).use { oos ->
                oos.writeObject(`object`)
            }
        }

        /**
         * Deserialize synset_id-to-offset map
         *
         * @param file in file
         * @return synset_id-to-offset map
         * @throws IOException            io exception
         * @throws ClassNotFoundException class not found exception
         */
        @Suppress("UNCHECKED_CAST")
        @Throws(IOException::class, ClassNotFoundException::class)
        fun deSerializedOffsets(file: File): Map<String, Long> {
            FileInputStream(file).use { s ->
                return deSerialize(s) as Map<String, Long>
            }
        }

        /**
         * Deserialize object
         *
         * @param is input stream
         * @return object
         * @throws IOException            io exception
         * @throws ClassNotFoundException class not found exception
         */
        @Throws(IOException::class, ClassNotFoundException::class)
        private fun deSerialize(`is`: InputStream): Any {
            ObjectInputStream(`is`).use { ois ->
                return ois.readObject()
            }
        }
    }
}
