/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Intermediate data, that are usually accumulated, not Pojos
 *
 * @author Bernard Bou
 */
public class Data
{
	private Data()
	{
	}

	/**
	 * Filter for noun synset pos
	 */
	public static final char NOUN_POS_FILTER = 'n';

	/**
	 * Filter for verb synset pos
	 */
	public static final char VERB_POS_FILTER = 'v';

	/**
	 * Filter for adj synset pos
	 */
	public static final char ADJ_POS_FILTER = 'a';

	/**
	 * Filter for adv synset pos
	 */
	public static final char ADV_POS_FILTER = 'r';

	/**
	 * Synset member lemma. Members are ordered lemmas. Lexid (from lexicographer file should not exceed 15 in compat mode)
	 */
	static class Member
	{
		protected final String lemma;

		protected final int lexid;

		public Member(String lemma, int lexid, boolean lexIdCompat)
		{
			super();
			this.lemma = lemma;
			if (lexIdCompat)
			{
				this.lexid = lexid % 16; // 16 -> 0
				if (lexid > 16)
				// throw new RuntimeException("Out of range lexid" + lemma + " " + lexid);
				{
					Tracing.psErr.printf("Out of range lexid %s: %d tweaked to %d%n", lemma, lexid, this.lexid);
				}
			}
			else
			{
				this.lexid = lexid;
			}
		}

		public String toWndbString(boolean lexIdCompat)
		{
			return String.format(lexIdCompat ? "%s %1X" : "%s %X", lemma, lexid);
		}

		@Override
		public String toString()
		{
			return String.format("Member %s lexid:%X", lemma, lexid);
		}
	}

	/**
	 * Adjective synset member lemmas with position constraint expressed (ip,p,a).
	 */
	static class AdjMember extends Member
	{
		private final String position;

		public AdjMember(String lemma, int lexid, String position, boolean lexIdCompat)
		{
			super(lemma, lexid, lexIdCompat);
			this.position = position;
		}

		@Override
		public String toWndbString(boolean lexIdCompat)
		{
			return String.format(lexIdCompat ? "%s(%s) %1X" : "%s(%s) %X", lemma, position, lexid);
		}

		@Override
		public String toString()
		{
			return String.format("Adj Member %s(%s) %X", lemma, position, lexid);
		}
	}

	/**
	 * Semantic or lexical relations
	 */
	static class Relation
	{
		final String ptrSymbol;

		final long targetOffset;

		final char pos;

		final char targetPos;

		/**
		 * The source/target field distinguishes lexical and semantic pointers.
		 * It is a four byte field, containing two two-digit hexadecimal integers.
		 * The first two digits indicate the word number in the current (source) synset.
		 * A value of 0000 means that pointer_symbol represents a semantic relation between
		 * the current (source) synset and the target synset indicated by synset_offset .
		 */
		final int sourceWordNum;

		/**
		 * The source/target field distinguishes lexical and semantic pointers.
		 * It is a four byte field, containing two two-digit hexadecimal integers.
		 * The last two digits indicate the word number in the target synset.
		 * A value of 0000 means that pointer_symbol represents a semantic relation between
		 * the current (source) synset and the target synset indicated by synset_offset .
		 */
		final int targetWordNum;

		/**
		 * Constructor
		 *
		 * @param type          type of relation @see Coder.codeRelation
		 * @param pos           source part of speech
		 * @param targetPos     target part of speech
		 * @param targetOffset  relation target offset
		 * @param sourceWordNum word number in source synset
		 * @param targetWordNum word number in target synset
		 * @param pointerCompat pointer compatibility
		 */
		public Relation(String type, char pos, char targetPos, long targetOffset, int sourceWordNum, int targetWordNum, final boolean pointerCompat) throws CompatException
		{
			super();
			this.ptrSymbol = Coder.codeRelation(type, pos, pointerCompat);
			this.pos = pos;
			this.targetPos = targetPos;
			this.targetOffset = targetOffset;
			this.sourceWordNum = sourceWordNum;
			this.targetWordNum = targetWordNum;
		}

		public String toWndbString()
		{
			return String.format("%s %08d %c %02x%02x", ptrSymbol, targetOffset, targetPos, sourceWordNum, targetWordNum);
		}

		@Override
		public String toString()
		{
			return String.format("Relation %s %08d %c %02x%02x", ptrSymbol, targetOffset, targetPos, sourceWordNum, targetWordNum);
		}
	}

	/**
	 * Verb (syntactic) frames
	 */
	static class Frame
	{
		public final int frameNum;

		public final int memberNum;

		/**
		 * Constructor
		 *
		 * @param frameNum  frame number
		 * @param memberNum 1-based lemma member number in synset this frame applies to
		 */
		public Frame(int frameNum, int memberNum)
		{
			super();
			this.frameNum = frameNum;
			this.memberNum = memberNum;
		}

		public String toWndbString()
		{
			return String.format("+ %02d %02x", frameNum, memberNum);
		}

		@Override
		public String toString()
		{
			return String.format("Frame %02d %02x", frameNum, memberNum);
		}
	}

	/**
	 * Verb (syntactic) frames, a list of frames mapped per given frameNum
	 */
	static class Frames extends HashMap<Integer, List<Frame>>
	{
		private static final long serialVersionUID = -3313309054723217964L;

		public Frames()
		{
			super();
		}

		public void add(final Frame frame)
		{
			List<Frame> frames2 = computeIfAbsent(frame.frameNum, k -> new ArrayList<>());
			frames2.add(frame);
		}

		/**
		 * Join frames. If a frame applies to all words, then frame num is zeroed
		 *
		 * @param pos          part of speech
		 * @param membersCount synset member count
		 * @return formatted verb frames
		 */
		public String toWndbString(char pos, int membersCount)
		{
			if (pos != 'v')
			{
				return "";
			}
			// compulsory for verbs even if empty
			if (size() < 1)
			{
				return "00";
			}
			List<Frame> resultFrames = new ArrayList<>();
			for (Entry<Integer, List<Frame>> entry : entrySet())
			{
				Integer frameNum = entry.getKey();
				List<Frame> framesWithFrameNum = entry.getValue();
				if (framesWithFrameNum.size() == membersCount)
				{
					resultFrames.add(new Frame(frameNum, 0));
				}
				else
				{
					resultFrames.addAll(framesWithFrameNum);
				}
			}
			return Formatter.joinNum(resultFrames, "%02d", Frame::toWndbString);
		}
	}
}
