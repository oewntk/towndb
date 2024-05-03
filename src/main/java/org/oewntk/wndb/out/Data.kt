/*
 * Copyright (c) 2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.wndb.out.Coder.codeRelation
import org.oewntk.wndb.out.Formatter.intFormat2
import org.oewntk.wndb.out.Formatter.intFormatHex2X
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
        lexid: Int,
        lexIdCompat: Boolean,
    ) {

        protected var lexid: Int = 0

        init {
            if (lexIdCompat) {
                this.lexid = lexid % 16 // 16 -> 0
                if (lexid > 16) // throw new RuntimeException("Out of range lexid" + lemma + " " + lexid);
                {
                    Tracing.psErr.printf("Out of range lexid %s: %d tweaked to %d%n", lemma, lexid, this.lexid)
                }
            } else {
                this.lexid = lexid
            }
        }

        open fun toWndbString(lexIdCompat: Boolean): String {
            return "$lemma ${lexidFormat(lexid, lexIdCompat)}"
        }

        override fun toString(): String {
            return "Member $lemma lexid:${lexidFormat(lexid)}"
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
    class Relation(
        type: String,
        pos: Char,
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
        pointerCompat: Boolean,

        ) {

        private val ptrSymbol: String = codeRelation(type, pos, pointerCompat)

        fun toWndbString(): String {
            return "$ptrSymbol ${offsetFormat(targetOffset)} $targetPos ${intFormatHex2X(sourceWordNum)}${intFormatHex2X(targetWordNum)}"
        }

        override fun toString(): String {
            return "Relation $ptrSymbol ${offsetFormat(targetOffset)} $targetPos ${intFormatHex2X(sourceWordNum)}${intFormatHex2X(targetWordNum)}"
        }
    }

    /**
     * Verb (syntactic) frames
     *
     * @param frameNum  frame number
     * @param memberNum 1-based lemma member number in synset this frame applies to
     */
    internal class Frame(
        val frameNum: Int,
        private val memberNum: Int,
    ) {

        fun toWndbString(): String {
            return "+ ${intFormat2(frameNum)} ${intFormatHex2x(memberNum)}"
        }

        override fun toString(): String {
            return "Frame ${intFormat2(frameNum)} ${intFormatHex2x(memberNum)}"
        }
    }

    /**
     * Verb (syntactic) frames, a list of frames mapped per given frameNum
     */
    internal class Frames : HashMap<Int, MutableList<Frame>>() {

        fun add(frame: Frame) {
            val frames2 = computeIfAbsent(frame.frameNum) { ArrayList() }
            frames2.add(frame)
        }

        /**
         * Join frames. If a frame applies to all words, then frame num is zeroed
         *
         * @param pos          part of speech
         * @param membersCount synset member count
         * @return formatted verb frames
         */
        fun toWndbString(pos: Char, membersCount: Int): String {
            if (pos != 'v') {
                return ""
            }
            // compulsory for verbs even if empty
            if (size < 1) {
                return "00"
            }
            val resultFrames: MutableList<Frame> = ArrayList()
            for ((frameNum, framesWithFrameNum) in entries) {
                if (framesWithFrameNum.size == membersCount) {
                    resultFrames.add(Frame(frameNum, 0))
                } else {
                    resultFrames.addAll(framesWithFrameNum)
                }
            }
            return resultFrames.joinToStringWithCount(countFormat = ::intFormat2) { it.toWndbString() }
        }
    }
}
