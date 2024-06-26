/*
 * Copyright (c) 2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.Category
import org.oewntk.wndb.out.Coder.codeRelation
import org.oewntk.wndb.out.Formatter.intFormat2
import org.oewntk.wndb.out.Formatter.intFormatHex2x
import org.oewntk.wndb.out.Formatter.joinToStringWithCount
import org.oewntk.wndb.out.Formatter.lexidFormat
import org.oewntk.wndb.out.Formatter.offsetFormat

/**
 * Intermediate data, that are usually accumulated, not Pojos
 *
 * @author Bernard Bou
 */
object Data {

    /**
     * Filter for noun synset pos
     */
    const val NOUN_POS_FILTER: Char = 'n'

    /**
     * Filter for verb synset pos
     */
    const val VERB_POS_FILTER: Char = 'v'

    /**
     * Filter for adj synset pos
     */
    const val ADJ_POS_FILTER: Char = 'a'

    /**
     * Filter for adv synset pos
     */
    const val ADV_POS_FILTER: Char = 'r'

    /**
     * Synset member lemma. Members are ordered lemmas. Lexid (from lexicographer file should not exceed 15 in compat mode)
     */
    internal open class Member(
        protected val lemma: String,
        protected val lexid: Int = 0,
    ) {

        constructor (
            lemma: String,
            lexid: Int,
            lexIdCompat: Boolean,
        ) : this(lemma, computeLexid(lexid, lexIdCompat, lemma))

        open fun toWndbString(lexIdCompat: Boolean): String {
            return "$lemma ${lexidFormat(lexid, lexIdCompat)}"
        }

        override fun toString(): String {
            return "Member $lemma lexid:${lexidFormat(lexid)}"
        }

        companion object {

            fun computeLexid(
                lexid: Int,
                lexIdCompat: Boolean,
                lemma: String,
            ): Int {
                return if (lexIdCompat) {
                    val result = lexid % 16 // 16 -> 0
                    if (lexid > 16) // throw RuntimeException("Out of range lexid for lemma $lemma: $lexid")
                    {
                        Tracing.psErr.println("Out of range lexid for lemma $lemma: $lexid tweaked to $lexid")
                    }
                    result
                } else {
                    lexid
                }
            }
        }
    }

    /**
     * Adjective synset member lemmas with position constraint expressed (ip,p,a).
     */
    internal class AdjMember(lemma: String, lexid: Int, private val position: String, lexIdCompat: Boolean) : Member(lemma, lexid, lexIdCompat) {

        override fun toWndbString(lexIdCompat: Boolean): String {
            return "$lemma($position) ${lexidFormat(lexid, lexIdCompat)}"
        }

        override fun toString(): String {
            return "Adj Member $lemma($position) ${lexidFormat(lexid)}"
        }
    }

    /**
     * Semantic or lexical relations
     */
    data class Relation(
        private val ptrSymbol: String,
        private val targetPos: Char,
        private val targetOffset: Long,

        /**
         * The source/target field distinguishes lexical and semantic pointers.
         * It is a four byte field, containing two two-digit hexadecimal integers.
         * The first two digits indicate the word number in the current (source) synset.
         * A value of 0000 means that pointer_symbol represents a semantic relation between
         * the current (source) synset and the target synset indicated by synset_offset .
         */
        private val sourceWordNum: Int,

        /**
         * The source/target field distinguishes lexical and semantic pointers.
         * It is a four byte field, containing two two-digit hexadecimal integers.
         * The last two digits indicate the word number in the target synset.
         * A value of 0000 means that pointer_symbol represents a semantic relation between
         * the current (source) synset and the target synset indicated by synset_offset .
         */
        private val targetWordNum: Int,

        ) {

        constructor (
            type: String,
            category: Category,
            targetPos: Char,
            targetOffset: Long,
            sourceWordNum: Int,
            targetWordNum: Int,
            pointerCompat: Boolean,
        ) : this(codeRelation(type, category, pointerCompat), targetPos, targetOffset, sourceWordNum, targetWordNum)

        fun toWndbString(): String {
            return "$ptrSymbol ${offsetFormat(targetOffset)} $targetPos ${intFormatHex2x(sourceWordNum)}${intFormatHex2x(targetWordNum)}"
        }

        override fun toString(): String {
            return "Relation $ptrSymbol ${offsetFormat(targetOffset)} $targetPos ${intFormatHex2x(sourceWordNum)}${intFormatHex2x(targetWordNum)}"
        }
    }

    /**
     * Verb (syntactic) frames
     *
     * @param frameNum  frame number
     * @param memberNum 1-based lemma member number in synset this frame applies to
     */
    internal data class VerbFrame(
        val frameNum: Int,
        private val memberNum: Int,
    ) {

        fun toWndbString(): String {
            return "+ ${intFormat2(frameNum)} ${intFormatHex2x(memberNum)}"
        }

        override fun toString(): String {
            return "Frame ${intFormat2(frameNum)} ${intFormatHex2x(memberNum)}"
        }

        companion object {

            /**
             * Join frames. If a frame applies to all words, then frame num is zeroed
             *
             * @param category     category: part of speech or ss_type
             * @param membersCount synset member count
             * @return formatted verb frames
             */
            fun Map<Int, List<VerbFrame>>.toWndbString(category: Category, membersCount: Int): String {

                if (category != 'v') {
                    return ""
                }
                // compulsory for verbs even if empty
                if (isEmpty()) {
                    return "00"
                }
                val allMembersFrames = entries
                    .filter { (_, framesWithFrameNum) -> framesWithFrameNum.size == membersCount }
                    .map { (frameNum, _) -> VerbFrame(frameNum, 0) }
                    .toList()
                val someMembersFrames = entries
                    .filter { (_, framesWithFrameNum) -> framesWithFrameNum.size != membersCount }
                    .flatMap { (_, framesWithFrameNum) -> framesWithFrameNum.toList() }
                    .toList()
                val resultFrames = allMembersFrames + someMembersFrames
                return resultFrames
                    .sortedBy { it.frameNum }
                    .joinToStringWithCount(countFormat = ::intFormat2) { it.toWndbString() }
            }

            private fun Map<Int, List<VerbFrame>>.toWndbString1(category: Category, membersCount: Int): String {
                if (category != 'v') {
                    return ""
                }
                // compulsory for verbs even if empty
                if (isEmpty()) {
                    return "00"
                }
                val resultFrames: MutableList<VerbFrame> = ArrayList()
                for ((frameNum, framesWithFrameNum) in entries) {
                    if (framesWithFrameNum.size == membersCount) {
                        resultFrames.add(VerbFrame(frameNum, 0))
                    } else {
                        resultFrames.addAll(framesWithFrameNum)
                    }
                }
                return resultFrames.joinToStringWithCount(countFormat = ::intFormat2) { it.toWndbString() }
            }
        }
    }
}
