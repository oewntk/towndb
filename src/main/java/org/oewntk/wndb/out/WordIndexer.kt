/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.Sense
import org.oewntk.model.SenseGroupings.sensesByLCLemmaAndPos
import org.oewntk.model.Synset
import java.io.PrintStream
import java.util.*
import java.util.stream.Collectors

/**
 * This class produces the 'index.{noun|verb|adj|adv}' files
 *
 * @property offsets offsets indexed by synset id key
 * @property flags   flags
 * @author Bernard Bou
 */
class WordIndexer(
	private val offsets: Map<String, Long>,
	private val flags: Int
) {
	/**
	 * This represents what is needed for a line in index.(noun|verb|adj|adv)
	 */
	private class IndexEntry
		(val pos: Char) {
		var taggedSensesCount: Int = 0
			private set

		val synsetIds: MutableSet<String> = LinkedHashSet()

		val relationPointers: MutableSet<String> = TreeSet()

		fun incTaggedCount() {
			taggedSensesCount += 1
		}
	}

	/**
	 * Make index
	 *
	 * @param ps          print stream
	 * @param senses      senses
	 * @param synsetsById synsets mapped by id
	 * @param posFilter   part-of-speech filter for lexes
	 * @return number of indexes
	 */
	fun make(ps: PrintStream, senses: Collection<Sense>, synsetsById: Map<String, Synset>, posFilter: Char): Long {
		val incompats: MutableMap<String, Int> = HashMap()

		// file header
		ps.print(Formatter.OEWN_HEADER)

		// collect entries
		val pointerCompat = (flags and Flags.pointerCompat) != 0
		val indexEntries: MutableMap<String, IndexEntry> = TreeMap()

		val groupedSenses = sensesByLCLemmaAndPos(senses)
		groupedSenses.entries.stream()
			.filter { it.key.pos == posFilter }
			.sorted(Comparator.comparing { it.key.lcLemma.replace(' ', '_') })
			.forEach {
				val k = it.key
				val kSenses = it.value.stream()
					.sorted(SenseComparator.WNDB_SENSE_ORDER)
					.collect(Collectors.toList())

				val lcLemma = k.lcLemma
				val pos = k.pos
				val ik = lcLemma.replace(' ', '_')
				val eik = Formatter.escape(ik)

				// init data mapped by lower-cased lemma
				val indexEntry = indexEntries.computeIfAbsent(eik) { IndexEntry(pos) }

				// synset ids
				collectSynsetIds(kSenses, indexEntry.synsetIds)

				// tag counts
				collectTagCounts(kSenses, indexEntry)

				// synset relations
				collectSynsetRelations(kSenses, synsetsById, pos, indexEntry.relationPointers, pointerCompat, incompats)

				// sense relations
				collectSenseRelations(kSenses, pos, indexEntry.relationPointers, pointerCompat, incompats)

				// print
				printIndexEntry(eik, indexEntry, ps)
			}

		// log
		reportIncompats(incompats)
		return indexEntries.size.toLong()
	}

	/**
	 * Collect synset ids
	 *
	 * @param senses    senses
	 * @param synsetIds set to collect to
	 */
	private fun collectSynsetIds(senses: List<Sense>, synsetIds: MutableSet<String>) {
		for (sense in senses) {
			// synsetid
			val synsetId = sense.synsetId
			// collect
			synsetIds.add(synsetId)
		}
	}

	/**
	 * Collect synset ids
	 *
	 * @param senses     senses
	 * @param indexEntry to collect to
	 */
	private fun collectTagCounts(senses: List<Sense>, indexEntry: IndexEntry) {
		for (sense in senses) {
			// synsetid
			val tagCount = sense.tagCount ?: continue
			// collect
			if (tagCount.count > 0) {
				indexEntry.incTaggedCount()
			}
		}
	}

	/**
	 * Collect synset relations
	 *
	 * @param senses        senses
	 * @param synsetsById   synsets by id
	 * @param pos           pos
	 * @param pointers      set of pointers to collect to
	 * @param pointerCompat pointer compatibility flag
	 * @param incompats     incompatibility log
	 */
	private fun collectSynsetRelations(senses: List<Sense>, synsetsById: Map<String, Synset>, pos: Char, pointers: MutableSet<String>, pointerCompat: Boolean, incompats: MutableMap<String, Int>) {
		for (sense in senses) {
			// synsetid
			val synsetId = sense.synsetId
			// synset relations
			val synset = synsetsById[synsetId]
			collectSynsetRelations(synset, pos, pointers, pointerCompat, incompats)
		}
	}

	/**
	 * Collect sense relations
	 *
	 * @param synset        synset
	 * @param pos           pos
	 * @param pointers      set of pointers to collect to
	 * @param pointerCompat pointer compatibility flag
	 * @param incompats     incompatibility log
	 */
	private fun collectSynsetRelations(synset: Synset?, pos: Char, pointers: MutableSet<String>, pointerCompat: Boolean, incompats: MutableMap<String, Int>) {
		val synsetRelations: Map<String, Set<String>>? = synset!!.relations
		if (!synsetRelations.isNullOrEmpty()) {
			for ((relationType) in synsetRelations) {
				var pointer: String
				try {
					pointer = Coder.codeRelation(relationType, pos, pointerCompat)
				} catch (e: CompatException) {
					val cause = e.cause!!.message!!
					val count = incompats.computeIfAbsent(cause) { 0 } + 1
					incompats[cause] = count
					continue
				} catch (e: IllegalArgumentException) {
					if (LOG) {
						Tracing.psErr.printf("[W] Discarded relation '%s'%n", relationType)
					}
					throw e
				}
				// collect
				pointers.add(pointer)
			}
		}
	}

	/**
	 * Collect sense relations
	 *
	 * @param senses        senses
	 * @param pos           pos
	 * @param pointers      set of pointers to collect to
	 * @param pointerCompat pointer compatibility flag
	 * @param incompats     incompatibility log
	 */
	private fun collectSenseRelations(senses: List<Sense>, pos: Char, pointers: MutableSet<String>, pointerCompat: Boolean, incompats: MutableMap<String, Int>) {
		for (lexSense in senses) {
			val senseRelations = lexSense.relations
			if (!senseRelations.isNullOrEmpty()) {
				for ((relationType) in senseRelations) {
					var pointer: String
					try {
						pointer = Coder.codeRelation(relationType, pos, pointerCompat)
					} catch (e: CompatException) {
						val cause = e.cause!!.message!!
						val count = incompats.computeIfAbsent(cause) { 0 } + 1
						incompats[cause] = count
						continue
					} catch (e: IllegalArgumentException) {
						if (LOG) {
							Tracing.psErr.printf("[W] Discarded relation '%s'%n", relationType)
						}
						continue
					}
					// collect
					pointers.add(pointer)
				}
			}
		}
	}

	/**
	 * Print index entry
	 *
	 * @param key        index key
	 * @param indexEntry index data
	 * @param ps         print stream
	 */
	private fun printIndexEntry(key: String, indexEntry: IndexEntry, ps: PrintStream) {
		val nSenses = indexEntry.synsetIds.size
		val ptrs = Formatter.joinNum(indexEntry.relationPointers, "%d") { it }
		val ofs = Formatter.join(indexEntry.synsetIds, " ", false) { String.format("%08d", offsets[it]) }
		val line = String.format(WORD_FORMAT, key, indexEntry.pos, nSenses, ptrs, nSenses, indexEntry.taggedSensesCount, ofs)
		ps.println(line)
	}

	/**
	 * Report incompatibilities to log
	 *
	 * @param incompats incompatibilities
	 */
	private fun reportIncompats(incompats: Map<String, Int>) {
		if (incompats.isNotEmpty()) {
			for ((key, value) in incompats) {
				Tracing.psErr.printf("[W] Incompatibilities '%s': %d%n", key, value)
			}
		}
	}

	companion object {
		private const val LOG = false

		/**
		 * Format in data file
		 * ```
		 * lemma
		 * pos
		 * synset_cnt
		 * ptrs=p_cnt  [ptr_symbol...]
		 * ofs=sense_cnt
		 * tag sense cnt (number of senses that are tagged)
		 * synset_offset  [synset_offset...]
		 * ```
		 */
		private const val WORD_FORMAT = "%s %s %d %s %d %d %s  "
	}
}