/*
 * Copyright (c) 2021-2021. Bernard Bou.
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
	private val flags: Int
) : Consumer<Model> {

	override fun accept(model: Model) {
		Tracing.psInfo.printf("[Model] %s%n", model.sources.contentToString())

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
		val offsets = GrindOffsets(model.lexesByLemma!!, model.synsetsById!!, model.sensesById!!, flags).compute()

		// Process
		data(outDir, model.lexesByLemma!!, model.synsetsById!!, model.sensesById!!, offsets)
		indexWords(outDir, model.senses, model.synsetsById!!, offsets)
		indexSenses(outDir, model.senses, offsets)
		morphs(outDir, model.lexesByLemma!!)
		templates(outDir, model.verbTemplatesById)
		indexTemplates(outDir, model.sensesById!!)
		verbFrames(outDir, model.verbFrames)
		tagcounts(outDir, model.sensesById!!)
		domains(outDir)
	}

	/**
	 * Grind data.{noun|verb|adj|adv}
	 *
	 * @param dir          output directory
	 * @param lexesByLemma lexes mapped by lemma
	 * @param synsetsById  synsets mapped by synset id
	 * @param sensesById   senses mapped by sense id
	 * @param offsets      offsets mapped by synset id
	 * @throws IOException io
	 */
	@Throws(IOException::class)
	fun data(
		dir: File,  //
		lexesByLemma: Map<String, Collection<Lex>>,  //
		synsetsById: Map<String, Synset>,  //
		sensesById: Map<String, Sense>,  //
		offsets: Map<String, Long>
	) {
		var nCount: Long
		var vCount: Long
		var aCount: Long
		var rCount: Long
		val grinder = GrindSynsets(lexesByLemma, synsetsById, sensesById, offsets, flags)
		PrintStream(FileOutputStream(File(dir, "data.noun")), true, StandardCharsets.UTF_8).use { ps ->
			nCount = grinder.makeData(ps, synsetsById, Data.NOUN_POS_FILTER)
			grinder.report()
		}
		PrintStream(FileOutputStream(File(dir, "data.verb")), true, StandardCharsets.UTF_8).use { ps ->
			vCount = grinder.makeData(ps, synsetsById, Data.VERB_POS_FILTER)
			grinder.report()
		}
		PrintStream(FileOutputStream(File(dir, "data.adj")), true, StandardCharsets.UTF_8).use { ps ->
			aCount = grinder.makeData(ps, synsetsById, Data.ADJ_POS_FILTER)
			grinder.report()
		}
		PrintStream(FileOutputStream(File(dir, "data.adv")), true, StandardCharsets.UTF_8).use { ps ->
			rCount = grinder.makeData(ps, synsetsById, Data.ADV_POS_FILTER)
			grinder.report()
		}
		val sum = nCount + vCount + aCount + rCount
		Tracing.psInfo.printf("Synsets: %d [n:%d v:%d a:%d r:%d]%n", sum, nCount, vCount, aCount, rCount)
	}

	/**
	 * Make word index
	 *
	 * @param dir         output directory
	 * @param senses      senses
	 * @param synsetsById synsets mapped by synset id
	 * @param offsets     offsets mapped by synset id
	 * @throws IOException io
	 */
	@Throws(IOException::class)
	fun indexWords(
		dir: File,
		senses: Collection<Sense>,
		synsetsById: Map<String, Synset>,
		offsets: Map<String, Long>
	) {
		var nCount: Long
		var vCount: Long
		var aCount: Long
		var rCount: Long
		val indexer = WordIndexer(offsets, flags)
		PrintStream(FileOutputStream(File(dir, "index.noun")), true, StandardCharsets.UTF_8).use { ps ->
			nCount = indexer.make(ps, senses, synsetsById, Data.NOUN_POS_FILTER)
		}
		PrintStream(FileOutputStream(File(dir, "index.verb")), true, StandardCharsets.UTF_8).use { ps ->
			vCount = indexer.make(ps, senses, synsetsById, Data.VERB_POS_FILTER)
		}
		PrintStream(FileOutputStream(File(dir, "index.adj")), true, StandardCharsets.UTF_8).use { ps ->
			aCount = indexer.make(ps, senses, synsetsById, Data.ADJ_POS_FILTER)
		}
		PrintStream(FileOutputStream(File(dir, "index.adv")), true, StandardCharsets.UTF_8).use { ps ->
			rCount = indexer.make(ps, senses, synsetsById, Data.ADV_POS_FILTER)
		}
		val sum = nCount + vCount + aCount + rCount
		Tracing.psInfo.printf("Indexes: %d [n:%d v:%d a:%d r:%d]%n", sum, nCount, vCount, aCount, rCount)
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
					ps.printf("%02d\t%s\t%d%n", it.value, it.key, posNameToInt(it.key.split("\\.".toRegex()).dropLastWhile { it2 -> it2.isEmpty() }.toTypedArray()[0]))
				}
		}
		Tracing.psInfo.printf("Lexfiles: %d%n", Coder.LEXFILE_TO_NUM.size)
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
				.forEach { ps.printf("%d %s%n", it.first, it.second.frame) }
		}
		Tracing.psInfo.printf("Verb frames: %d%n", verbFrames.size)
	}

	companion object {
		/**
		 * Grind {noun|verb|adj|adv}.exc
		 *
		 * @param lexesByLemma lexes mapped by lemma
		 * @param dir          output directory
		 * @throws IOException io
		 */
		@Throws(IOException::class)
		fun morphs(dir: File, lexesByLemma: Map<String, Collection<Lex>>) {
			var nCount: Long
			var vCount: Long
			var aCount: Long
			var rCount: Long
			val grinder = GrindMorphs()
			PrintStream(FileOutputStream(File(dir, "noun.exc")), true, StandardCharsets.UTF_8).use { ps ->
				nCount = grinder.makeMorph(ps, lexesByLemma, Data.NOUN_POS_FILTER)
			}
			PrintStream(FileOutputStream(File(dir, "verb.exc")), true, StandardCharsets.UTF_8).use { ps ->
				vCount = grinder.makeMorph(ps, lexesByLemma, Data.VERB_POS_FILTER)
			}
			PrintStream(FileOutputStream(File(dir, "adj.exc")), true, StandardCharsets.UTF_8).use { ps ->
				aCount = grinder.makeMorph(ps, lexesByLemma, Data.ADJ_POS_FILTER)
			}
			PrintStream(FileOutputStream(File(dir, "adv.exc")), true, StandardCharsets.UTF_8).use { ps ->
				rCount = grinder.makeMorph(ps, lexesByLemma, Data.ADV_POS_FILTER)
			}
			val sum = nCount + vCount + aCount + rCount
			Tracing.psInfo.printf("Morphs: %d [n:%d v:%d a:%d r:%d]%n", sum, nCount, vCount, aCount, rCount)
		}

		/**
		 * Grind sentidx.vrb
		 *
		 * @param dir        output directory
		 * @param sensesById senses mapped by id
		 * @throws IOException io
		 */
		@Throws(IOException::class)
		fun indexTemplates(dir: File, sensesById: Map<String, Sense>) {
			val indexer = TemplateIndexer()
			PrintStream(FileOutputStream(File(dir, "sentidx.vrb")), true, StandardCharsets.UTF_8).use { ps ->
				indexer.makeIndex(ps, sensesById)
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
		 * @param dir        output directory
		 * @param sensesById senses mapped by id
		 * @throws IOException io
		 */
		@Throws(IOException::class)
		fun tagcounts(dir: File, sensesById: Map<String, Sense>) {
			val grinder = GrindTagCounts()
			PrintStream(FileOutputStream(File(dir, "cntlist")), true, StandardCharsets.UTF_8).use { ps ->
				grinder.makeTagCount(ps, sensesById)
			}
			PrintStream(FileOutputStream(File(dir, "cntlist.rev")), true, StandardCharsets.UTF_8).use { ps ->
				grinder.makeTagCountRev(ps, sensesById)
			}
		}

		/**
		 * Pos name to int
		 *
		 * @param posName pos name
		 * @return integer code
		 */
		private fun posNameToInt(posName: String): Int {
			when (posName) {
				"noun" -> return 1
				"verb" -> return 2
				"adj" -> return 3
				"adv" -> return 4
			}
			return 0
		}

		// name, frameid
		private val VERBFRAME_VALUES = arrayOf(
			arrayOf("vii", 1),  //
			arrayOf("via", 2),  //
			arrayOf("nonreferential", 3),  //
			arrayOf("vii-pp", 4),  //
			arrayOf("vtii-adj", 5),  //
			arrayOf("vii-adj", 6),  //
			arrayOf("via-adj", 7),  //
			arrayOf("vtai", 8),  //
			arrayOf("vtaa", 9),  //
			arrayOf("vtia", 10),  //
			arrayOf("vtii", 11),  //
			arrayOf("vii-to", 12),  //
			arrayOf("via-on-inanim", 13),  //
			arrayOf("ditransitive", 14),  //
			arrayOf("vtai-to", 15),  //
			arrayOf("vtai-from", 16),  //
			arrayOf("vtaa-with", 17),  //
			arrayOf("vtaa-of", 18),  //
			arrayOf("vtai-on", 19),  //
			arrayOf("vtaa-pp", 20),  //
			arrayOf("vtai-pp", 21),  //
			arrayOf("via-pp", 22),  //
			arrayOf("vibody", 23),  //
			arrayOf("vtaa-to-inf", 24),  //
			arrayOf("vtaa-inf", 25),  //
			arrayOf("via-that", 26),  //
			arrayOf("via-to", 27),  //
			arrayOf("via-to-inf", 28),  //
			arrayOf("via-whether-inf", 29),  //
			arrayOf("vtaa-into-ger", 30),  //
			arrayOf("vtai-with", 31),  //
			arrayOf("via-inf", 32),  //
			arrayOf("via-ger", 33),  //
			arrayOf("nonreferential-sent", 34),  //
			arrayOf("vii-inf", 35),  //
			arrayOf("via-at", 36),  //
			arrayOf("via-for", 37),  //
			arrayOf("via-on-anim", 38),  //
			arrayOf("via-out-of", 39),  //
		)

		/**
		 * Map frame id (via, ...) to numeric id
		 */
		private val VERB_FRAME_ID_TO_NIDS = sequenceOf(*VERBFRAME_VALUES)
			.map { it[0] as String to it[1] as Int }
			.toMap()

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
