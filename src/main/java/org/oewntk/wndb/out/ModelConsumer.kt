/*
 * Copyright (c) 2021-2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.*
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.function.Consumer

/**
 * Main class that generates the WN database in the WNDB format as per wndb(5WN)
 *
 * @param outDir output directory
 * @param flags  flags
 *
 * @author Bernard Bou
 * @see "https://wordnet.princeton.edu/documentation/wndb5wn"
 */
class ModelConsumer(
    private val outDir: File,
    private val flags: Int,
) : Consumer<Model> {

    override fun accept(model: Model) {

        Tracing.psInfo.println("[Model] ${model.sources.contentToString()}")

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
    fun grind(model: Model) {
        // Output
        if (!outDir.exists()) {
            outDir.mkdirs()
        }

        // Compute synset offsets
        val offsets = GrindOffsets(model.synsets, model.lexResolver, model.synsetResolver, model.senseResolver, flags).compute()

        // Process
        data(outDir, model.synsets, model.lexResolver, model.synsetResolver, model.senseResolver, offsets)
        indexWords(outDir, model.senses, model.synsetResolver, offsets)
        indexSenses(outDir, model.senses, offsets)
        morphs(outDir, model.lexEntries)
        templates(outDir, model.verbTemplatesById!!)
        indexTemplates(outDir, model.senses)
        verbFrames(outDir, model.verbFrames)
        tagcounts(outDir, model.senses)
        domains(outDir)
    }

    /**
     * Grind data.{noun|verb|adj|adv}
     *
     * @param dir output directory
     * @param synsets all synsets
     * @param lexResolver lexes mapped by lemma
     * @param synsetResolver  synsets mapped by synset id
     * @param senseResolver   senses mapped by sense id
     * @param offsets      offsets mapped by synset id
     * @throws IOException io
     */
    @Throws(IOException::class)
    fun data(
        dir: File,
        synsets: Collection<Synset>,
        lexResolver: (Lemma) -> Collection<Lex>,
        synsetResolver: (SynsetId) -> Synset,
        senseResolver: (SenseKey) -> Sense,
        offsets: Map<String, Long>,
    ) {
        var nCount: Int
        var vCount: Int
        var aCount: Int
        var rCount: Int
        val grinder = GrindSynsets(synsets, lexResolver, synsetResolver, senseResolver, offsets, flags)
        PrintStream(FileOutputStream(File(dir, "data.${PartOfSpeech.N.fullName}")), true, StandardCharsets.UTF_8).use { ps ->
            nCount = grinder.makeData(ps, synsets, synsetResolver, PartOfSpeech.N)
            grinder.report()
        }
        PrintStream(FileOutputStream(File(dir, "data.${PartOfSpeech.V.fullName}")), true, StandardCharsets.UTF_8).use { ps ->
            vCount = grinder.makeData(ps, synsets, synsetResolver, PartOfSpeech.V)
            grinder.report()
        }
        PrintStream(FileOutputStream(File(dir, "data.${PartOfSpeech.A.fullName}")), true, StandardCharsets.UTF_8).use { ps ->
            aCount = grinder.makeData(ps, synsets, synsetResolver, PartOfSpeech.A)
            grinder.report()
        }
        PrintStream(FileOutputStream(File(dir, "data.${PartOfSpeech.R.fullName}")), true, StandardCharsets.UTF_8).use { ps ->
            rCount = grinder.makeData(ps, synsets, synsetResolver, PartOfSpeech.R)
            grinder.report()
        }
        val sum = nCount + vCount + aCount + rCount
        Tracing.psInfo.println("Synsets: $sum [n:$nCount v:$vCount a:$aCount r:$rCount]")
    }

    /**
     * Make word index
     *
     * @param dir            output directory
     * @param senses         senses
     * @param synsetResolver synsets mapped by synset id
     * @param offsets        offsets mapped by synset id
     * @throws IOException   io
     */
    @Throws(IOException::class)
    fun indexWords(
        dir: File,
        senses: Collection<Sense>,
        synsetResolver: (SynsetId) -> Synset,
        offsets: Map<String, Long>,
    ) {
        var nCount: Long
        var vCount: Long
        var aCount: Long
        var rCount: Long
        val indexer = WordIndexer(offsets, flags)
        PrintStream(FileOutputStream(File(dir, "index.${PartOfSpeech.N.fullName}")), true, StandardCharsets.UTF_8).use { ps ->
            nCount = indexer.make(ps, senses, synsetResolver, PartOfSpeech.N)
        }
        PrintStream(FileOutputStream(File(dir, "index.${PartOfSpeech.V.fullName}")), true, StandardCharsets.UTF_8).use { ps ->
            vCount = indexer.make(ps, senses, synsetResolver, PartOfSpeech.V)
        }
        PrintStream(FileOutputStream(File(dir, "index.${PartOfSpeech.A.fullName}")), true, StandardCharsets.UTF_8).use { ps ->
            aCount = indexer.make(ps, senses, synsetResolver, PartOfSpeech.A)
        }
        PrintStream(FileOutputStream(File(dir, "index.${PartOfSpeech.R.fullName}")), true, StandardCharsets.UTF_8).use { ps ->
            rCount = indexer.make(ps, senses, synsetResolver, PartOfSpeech.R)
        }
        val sum = nCount + vCount + aCount + rCount
        Tracing.psInfo.println("Indexes: $sum [n:$nCount v:$vCount a:$aCount r:$rCount]")
    }

    /**
     * Grind index.sense
     *
     * @param dir     output directory
     * @param senses  senses
     * @param offsets offsets mapped by synsetId
     * @throws IOException io
     */
    @Throws(IOException::class)
    fun indexSenses(dir: File, senses: Collection<Sense>, offsets: Map<String, Long>) {
        PrintStream(FileOutputStream(File(dir, "index.sense")), true, StandardCharsets.UTF_8).use { ps ->
            SenseIndexer(flags, offsets).make(ps, senses)
        }
    }

    /**
     * Grind lexdomains
     *
     * @param dir output directory
     */
    @Throws(FileNotFoundException::class)
    private fun domains(dir: File) {
        PrintStream(FileOutputStream(File(dir, "lexnames")), true, StandardCharsets.UTF_8).use { ps ->
            Coder.LEXFILE_TO_NUM.entries
                .toList()
                .sortedBy { it.value }
                .forEach {
                    val posId = posNameToInt(it.key.split("\\.".toRegex()).dropLastWhile { it2 -> it2.isEmpty() }.toTypedArray()[0])
                    ps.println("${String.format("%02d", it.value)}\t${it.key}\t$posId")
                }
        }
        Tracing.psInfo.println("Lexfiles: ${Coder.LEXFILE_TO_NUM.size}")
    }

    /**
     * Grind verb frames
     *
     * @param dir        output directory
     * @param verbFrames verb frames
     */
    @Throws(FileNotFoundException::class)
    private fun verbFrames(dir: File, verbFrames: Collection<VerbFrame>) {
        PrintStream(FileOutputStream(File(dir, "verb.Framestext")), true, StandardCharsets.UTF_8).use { ps ->
            verbFrames
                .withIndex()
                .map { (i, vf) -> getVerbFrameNID(vf, i + 1) to vf }
                .sortedBy { it.first }
                .forEach { ps.println("${it.first} ${it.second.frame}") }
        }
        Tracing.psInfo.println("Verb frames: ${verbFrames.size}")
    }

    companion object {

        /**
         * Grind {noun|verb|adj|adv}.exc
         *
         * @param lexEntries lexes mapped by lemma
         * @param dir output directory
         * @throws IOException io
         */
        @Throws(IOException::class)
        fun morphs(dir: File, lexEntries: Sequence<LexEntry>) {
            var nCount: Int
            var vCount: Int
            var aCount: Int
            var rCount: Int
            val grinder = GrindMorphs()
            PrintStream(FileOutputStream(File(dir, "${PartOfSpeech.N.fullName}.exc")), true, StandardCharsets.UTF_8)
                .use { ps ->
                    nCount = grinder.makeMorph(ps, lexEntries, PartOfSpeech.N)
                }
            PrintStream(FileOutputStream(File(dir, "${PartOfSpeech.V.fullName}.exc")), true, StandardCharsets.UTF_8)
                .use { ps ->
                    vCount = grinder.makeMorph(ps, lexEntries, PartOfSpeech.V)
                }
            PrintStream(FileOutputStream(File(dir, "${PartOfSpeech.A.fullName}.exc")), true, StandardCharsets.UTF_8)
                .use { ps ->
                    aCount = grinder.makeMorph(ps, lexEntries, PartOfSpeech.A)
                }
            PrintStream(FileOutputStream(File(dir, "${PartOfSpeech.R.fullName}.exc")), true, StandardCharsets.UTF_8)
                .use { ps ->
                    rCount = grinder.makeMorph(ps, lexEntries, PartOfSpeech.R)
                }
            val sum = nCount + vCount + aCount + rCount
            Tracing.psInfo.println("Morphs: $sum [n:$nCount v:$vCount a:$aCount r:$rCount]")
        }

        /**
         * Grind sentidx.vrb
         *
         * @param dir          output directory
         * @param senses       senses
         * @throws IOException io
         */
        @Throws(IOException::class)
        fun indexTemplates(dir: File, senses: Collection<Sense>) {
            val indexer = TemplateIndexer()
            PrintStream(FileOutputStream(File(dir, "sentidx.vrb")), true, StandardCharsets.UTF_8).use { ps ->
                indexer.makeIndex(ps, senses)
            }
        }

        /**
         * Grind sent.vrb
         *
         * @param dir               output directory
         * @param verbTemplatesById verb templates mapped by id
         * @throws IOException io
         */
        @Throws(IOException::class)
        fun templates(dir: File, verbTemplatesById: Map<Int, VerbTemplate>) {
            val grinder = GrindVerbTemplates()
            PrintStream(FileOutputStream(File(dir, "sents.vrb")), true, StandardCharsets.UTF_8).use { ps ->
                grinder.makeTemplates(ps, verbTemplatesById)
            }
        }

        /**
         * Grind cntlist cntlist.rev
         *
         * @param dir    output directory
         * @param senses senses
         * @throws IOException io
         */
        @Throws(IOException::class)
        fun tagcounts(dir: File, senses: Collection<Sense>) {
            val grinder = GrindTagCounts()
            PrintStream(FileOutputStream(File(dir, "cntlist")), true, StandardCharsets.UTF_8).use { ps ->
                grinder.makeTagCount(ps, senses)
            }
            PrintStream(FileOutputStream(File(dir, "cntlist.rev")), true, StandardCharsets.UTF_8).use { ps ->
                grinder.makeTagCountRev(ps, senses)
            }
        }

        /**
         * Pos name to int
         *
         * @param posName pos name
         * @return integer code
         */
        private fun posNameToInt(posName: String): Int {
            return PartOfSpeech.fromFullName(posName).ordinal + 1
        }

        // name, frameid
        private val VERBFRAME_VALUES = arrayOf(
            "vii" to 1,
            "via" to 2,
            "nonreferential" to 3,
            "vii-pp" to 4,
            "vtii-adj" to 5,
            "vii-adj" to 6,
            "via-adj" to 7,
            "vtai" to 8,
            "vtaa" to 9,
            "vtia" to 10,
            "vtii" to 11,
            "vii-to" to 12,
            "via-on-inanim" to 13,
            "ditransitive" to 14,
            "vtai-to" to 15,
            "vtai-from" to 16,
            "vtaa-with" to 17,
            "vtaa-of" to 18,
            "vtai-on" to 19,
            "vtaa-pp" to 20,
            "vtai-pp" to 21,
            "via-pp" to 22,
            "vibody" to 23,
            "vtaa-to-inf" to 24,
            "vtaa-inf" to 25,
            "via-that" to 26,
            "via-to" to 27,
            "via-to-inf" to 28,
            "via-whether-inf" to 29,
            "vtaa-into-ger" to 30,
            "vtai-with" to 31,
            "via-inf" to 32,
            "via-ger" to 33,
            "nonreferential-sent" to 34,
            "vii-inf" to 35,
            "via-at" to 36,
            "via-for" to 37,
            "via-on-anim" to 38,
            "via-out-of" to 39,
        )

        /**
         * Map frame id (via, ...) to numeric id
         */
        private val VERB_FRAME_ID_TO_NIDS = sequenceOf(*VERBFRAME_VALUES).associate { it.first to it.second }

        /**
         * Get verb frame nid
         * @param vf verb frame
         * @param index index if map lookup fails
         * @return map lookup, 100 + index if it fails
         */
        private fun getVerbFrameNID(vf: VerbFrame, index: Int): Int {
            val id = vf.id
            val nid = VERB_FRAME_ID_TO_NIDS[id]
            if (nid != null) {
                return nid
            }
            return 100 + index
        }
    }
}
