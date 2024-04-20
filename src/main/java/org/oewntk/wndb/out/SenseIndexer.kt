package org.oewntk.wndb.out

import org.oewntk.model.Sense
import org.oewntk.model.SenseGroupings.KeyLCLemmaAndPos.Companion.of
import org.oewntk.model.SenseGroupings.sensesByLCLemmaAndPos
import java.io.PrintStream
import java.util.stream.Collectors

/**
 * This class produces the 'index.sense' file
 *
 * @param flags   flags
 * @param offsets synset offsets map indexed by synsetid key
 */
class SenseIndexer(
	private val flags: Int,
	private val offsets: Map<String, Long>
) {
	/**
	 * Make senses
	 *
	 * @param ps     print stream
	 * @param senses senses
	 */
	fun make(ps: PrintStream, senses: Collection<Sense>) {

		val groupedSenses = sensesByLCLemmaAndPos(senses)

		// collect
		senses.stream() //
			.sorted(Comparator.comparing(Sense::senseKey))
			.forEach { sense: Sense ->

				// sense
				val sensekey = sense.senseKey

				// offset
				val synsetId = sense.synsetId
				val offset = offsets[synsetId]!!

				// sense num
				val senseNum: Int
				if ((flags and Flags.noReIndex) == 0) {
					val k = of(sense)
					// this will yield diverging sensenums to senses varying only in lemma, not synsetid target, e.g. a%1:10:00:: and a%1:10:01::
					// var kSenses = groupedSenses.get(k).stream().sorted(SenseGroupings.byDecreasingTagCount).collect(Collectors.toList());
					// senseNum = kSenses.indexOf(sense) + 1;
					// the following will yield the same sensenum to senses varying only in lemma, not synsetid target, e.g. a%1:10:00:: and a%1:10:01::
					val kTargetSynsetIds = groupedSenses[k]!!.stream().sorted(SenseComparator.WNDB_SENSE_ORDER).map { s: Sense -> s.synsetId }.distinct().collect(Collectors.toList())
					senseNum = kTargetSynsetIds.indexOf(sense.synsetId) + 1
				} else {
					senseNum = sense.lexIndex + 1
				}

				// tag count
				val tagCount = sense.intTagCount

				// print
				printSenseEntry(sensekey, offset, senseNum, tagCount, ps)
			}

		// log
		Tracing.psInfo.printf("Senses: %d%n", senses.size)
	}

	/**
	 * Print sense entry
	 *
	 * @param sensekey sensekey
	 * @param offset   offset
	 * @param senseNum sense number (index)
	 * @param tagCount tag count
	 * @param ps       print stream
	 */
	private fun printSenseEntry(sensekey: String, offset: Long, senseNum: Int, tagCount: Int, ps: PrintStream) {
		val line = String.format(SENSE_FORMAT, sensekey, offset, senseNum, tagCount)
		ps.println(line)
	}

	companion object {

		/**
		 * Format in data file
		 * ```
		 * sense_key
		 * synset_offset
		 * sense_number
		 * tag_cnt
		 * ```
		 */
		private const val SENSE_FORMAT = "%s %08d %d %d"
	}
}
