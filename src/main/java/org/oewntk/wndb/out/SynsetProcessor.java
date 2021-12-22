/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import org.oewntk.model.Lex;
import org.oewntk.model.Sense;
import org.oewntk.model.Synset;
import org.oewntk.wndb.out.Data.*;

import java.util.*;
import java.util.function.ToLongFunction;

/**
 * This abstract class iterates over the synsets to produce a line of data. The real classes implement some functions differently.
 *
 * @author Bernard Bou
 */
public abstract class SynsetProcessor
{
	private static final boolean LOG_DISCARDED = true;

	private static final boolean LOG_DUPLICATED = true;

	/**
	 * Format in data file
	 */
	private static final String SYNSET_FORMAT = "%08d %02d %c %s %s%s | %s%s  \n";
	// offset
	// lexfile num
	// pos
	// members
	// relations
	// frames
	// definition
	// example

	/**
	 * Lexes mapped by lemma
	 */
	protected final Map<String, Collection<Lex>> lexesByLemma;

	/**
	 * Synsets mapped by id
	 */
	protected final Map<String, Synset> synsetsById;

	/**
	 * Senses mapped by id
	 */
	protected final Map<String, Sense> sensesById;

	/**
	 * Flags
	 */
	protected final int flags;

	/**
	 * Function that, when applied to a synsetId, yields the synset offset in the data files. May be dummy constant function.
	 */
	protected final ToLongFunction<String> offsetFunction;

	/**
	 * Report incompatibility counts (indexed by cause)
	 */
	protected final Map<String, Integer> incompats;

	/**
	 * Log error flag (avoid duplicate messages)
	 *
	 * @return whether to log errors
	 */
	@SuppressWarnings("SameReturnValue")
	protected abstract boolean log();

	/**
	 * Constructor
	 *
	 * @param lexesByLemma   lexes mapped by lemma
	 * @param synsetsById    synsets mapped by id
	 * @param sensesById     senses mapped by id
	 * @param offsetFunction function that, when applied to a synsetId, yields the synset offset in the data files. May be dummy constant function.
	 * @param flags          flags
	 */
	protected SynsetProcessor(Map<String, Collection<Lex>> lexesByLemma, Map<String, Synset> synsetsById, Map<String, Sense> sensesById, ToLongFunction<String> offsetFunction, int flags)
	{
		this.lexesByLemma = lexesByLemma;
		this.synsetsById = synsetsById;
		this.sensesById = sensesById;
		this.offsetFunction = offsetFunction;
		this.flags = flags;
		this.incompats = new HashMap<>();
	}

	/**
	 * Intermediate class to detect duplicates
	 */
	static class RelationData implements Comparable<RelationData>
	{
		public final boolean isSenseRelation;

		public final String relType;

		public final String target;

		RelationData(boolean isSenseRelation, String relType, String target)
		{
			this.isSenseRelation = isSenseRelation;
			this.relType = relType;
			this.target = target;
		}

		// identity

		@Override
		public boolean equals(Object other)
		{
			if (this == other)
			{
				return true;
			}
			if (other == null || getClass() != other.getClass())
			{
				return false;
			}
			RelationData that = (RelationData) other;
			return isSenseRelation == that.isSenseRelation && Objects.equals(relType, that.relType) && Objects.equals(target, that.target);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(isSenseRelation, relType, target);
		}

		// order

		@Override
		public int compareTo(RelationData that)
		{
			int c = Boolean.compare(this.isSenseRelation, that.isSenseRelation);
			if (c != 0)
			{
				return c;
			}
			c = this.relType.compareTo(that.relType);
			if (c != 0)
			{
				return c;
			}
			return this.relType.compareTo(that.relType);
		}

		// string

		@Override
		public String toString()
		{
			return (this.isSenseRelation ? "SenseRelation" : "SynsetRelation") + " relType=" + this.relType + " target=" + this.target;
		}
	}

	private Member buildMember(Sense sense)
	{
		// lexid
		int lexid = sense.findLexid();

		// adjPosition
		String adjPosition = sense.getAdjPosition();

		// lex
		Lex lex = sense.getLex();

		// lemma
		String lemma = lex.getLemma();

		String escaped = Formatter.escape(lemma);
		boolean lexIdCompat = (flags & Flags.lexIdCompat) != 0;
		return adjPosition == null || adjPosition.isEmpty() ? new Member(escaped, lexid, lexIdCompat) : new AdjMember(escaped, lexid, adjPosition, lexIdCompat);
	}

	/**
	 * Get data and yield line
	 *
	 * @param synset synset
	 * @param offset allocated offset for the synset
	 * @return line
	 */
	public String getData(Synset synset, long offset)
	{
		// init
		List<Relation> relations = new ArrayList<>();
		Frames frames = new Frames();

		// attribute data
		char type = synset.getType();

		// build members ordered set
		List<Member> members = new ArrayList<>();
		for (String lemma : synset.getMembers())
		{
			Sense sense = synset.findSenseOf(lemma, lexesByLemma);
			assert sense != null : String.format("find sense of '%s' in synset %s", lemma, synset);
			Member member = buildMember(sense);
			members.add(member);
		}

		// definition and examples
		// allow multiple definitions and join them
		String[] definitions = synset.getDefinitions();
		String[] examples = synset.getExamples();

		// lexfile num
		int lexfileNum = buildLexfileNum(synset);

		// synset relations
		Map<String, Set<String>> modelSynsetRelations = synset.getRelations();
		if (modelSynsetRelations != null && modelSynsetRelations.size() > 0)
		{
			Set<RelationData> relationDataSet = new LinkedHashSet<>();
			for (Map.Entry<String, Set<String>> entry : modelSynsetRelations.entrySet())
			{
				String relationType = entry.getKey();
				Set<String> values = entry.getValue();
				for (String targetSynsetId : values)
				{
					RelationData relation = new RelationData(false, relationType, targetSynsetId);
					boolean wasThere = !relationDataSet.add(relation);
					if (wasThere && LOG_DUPLICATED && log())
					{
						Tracing.psErr.printf("[W] Synset %s has duplicate %s%n", synset.getSynsetId(), relation);
					}
				}
			}
			boolean pointerCompat = (flags & Flags.pointerCompat) != 0;
			for (RelationData relationData : relationDataSet)
			{
				Synset targetSynset = synsetsById.get(relationData.target);

				long targetOffset = offsetFunction.applyAsLong(relationData.target);
				char targetType = targetSynset.getType();
				Relation relation;
				try
				{
					relation = new Relation(relationData.relType, type, targetType, targetOffset, 0, 0, pointerCompat);
				}
				catch (CompatException e)
				{
					String cause = e.getCause().getMessage();
					int count = this.incompats.computeIfAbsent(cause, c -> 0) + 1;
					this.incompats.put(cause, count);
					continue;
				}
				catch (IllegalArgumentException e)
				{
					if (LOG_DISCARDED && log())
					{
						// String cause = e.getClass().getName() + ' ' + e.getMessage();
						Tracing.psErr.printf("[W] Discarded relation '%s' synset=%s offset=%d%n", relationData.relType, synset.getSynsetId(), offset);
					}
					throw e;
				}
				relations.add(relation);
			}
		}



		// senses that have this synset as target in "synset" attribute
		Sense[] senses = synset.findSenses(lexesByLemma);
		assert senses.length > 0;

		// iterate senses
		for (Sense sense : senses)
		{
			boolean verbFrameCompat = (flags & Flags.verbFrameCompat) != 0;

			// verb frames attribute
			String[] verbFrameIds = sense.getVerbFrames();
			if (verbFrameIds != null && verbFrameIds.length > 0)
			{
				for (String verbframeId : verbFrameIds)
				{
					try
					{
						Frame frame = new Frame(Coder.codeFrameId(verbframeId, verbFrameCompat), sense.findSynsetIndex(synsetsById));
						frames.add(frame);
					}
					catch (CompatException e)
					{
						String cause = e.getCause().getMessage();
						int count = this.incompats.computeIfAbsent(cause, c -> 0) + 1;
						this.incompats.put(cause, count);
					}
				}
			}

			// sense relations
			Map<String, List<String>> modelSenseRelations = sense.getRelations();
			if (modelSenseRelations != null && modelSenseRelations.size() > 0)
			{
				String lemma = sense.getLemma();

				Set<RelationData> senseRelationDataSet = new LinkedHashSet<>();
				for (Map.Entry<String, List<String>> entry : modelSenseRelations.entrySet())
				{
					String relationType = entry.getKey();
					List<String> values = entry.getValue();
					for (String targetSenseId : values)
					{
						RelationData relation = new RelationData(true, relationType, targetSenseId);
						boolean wasThere = !senseRelationDataSet.add(relation);
						if (wasThere  && LOG_DUPLICATED && log())
						{
							Tracing.psErr.printf("[W] Sense %s has duplicate %s%n", sense.getSenseId(), relation);
						}
					}
				}

				for (RelationData relationData : senseRelationDataSet)
				{
					Sense targetSense = sensesById.get(relationData.target);
					String targetSynsetId = targetSense.getSynsetId();
					Synset targetSynset = synsetsById.get(targetSynsetId);

					int memberIndex = synset.findIndexOfMember(lemma);

					Relation relation;
					try
					{
						relation = buildSenseRelation(relationData.relType, type, memberIndex, targetSense, targetSynset, targetSynsetId);
					}
					catch (CompatException e)
					{
						String cause = e.getCause().getMessage();
						int count = this.incompats.computeIfAbsent(cause, c -> 0) + 1;
						this.incompats.put(cause, count);
						continue;
					}
					catch (IllegalArgumentException e)
					{
						if (LOG_DISCARDED && log())
						{
							// String cause = e.getClass().getName() + ' ' + e.getMessage();
							Tracing.psErr.printf("[W] Discarded relation '%s' synset=%s offset=%d%n", relationData.relType, synset.getSynsetId(), offset);
						}
						// throw e;
						continue;
					}
					relations.add(relation);
				}
			}
		}

		// assemble
		boolean lexIdCompat = (flags & Flags.lexIdCompat) != 0;
		String membersData = Formatter.joinNum(members, "%02x", m -> m.toWndbString(lexIdCompat));
		String relatedData = Formatter.joinNum(relations, "%03d", Relation::toWndbString);
		String verbframesData = frames.toWndbString(type, members.size());
		if (!verbframesData.isEmpty())
		{
			verbframesData = ' ' + verbframesData;
		}
		assert definitions != null;
		String definitionsData = Formatter.join(definitions, "; ");
		String examplesData = examples == null || examples.length == 0 ? "" : "; " + Formatter.joinAndQuote(examples, " ", false, null);
		return String.format(SYNSET_FORMAT, offset, lexfileNum, type, membersData, relatedData, verbframesData, definitionsData, examplesData);
	}

	/**
	 * Build relation
	 *
	 * @param type           relation type
	 * @param pos            part of speech
	 * @param lemmaIndex     lemmaIndex
	 * @param targetSense    target sense
	 * @param targetSynset   target synset
	 * @param targetSynsetId target synsetid
	 * @return relation
	 * @throws CompatException when relation is not legacy compatible
	 */
	protected Relation buildSenseRelation(String type, char pos, int lemmaIndex, Sense targetSense, Synset targetSynset, String targetSynsetId) throws CompatException
	{
		// target lemma
		String targetLemma = targetSense.getLemma();

		// which
		int targetMemberNum = targetSynset.findIndexOfMember(targetLemma);
		char targetType = targetSynset.getType();
		long targetOffset = this.offsetFunction.applyAsLong(targetSynsetId);
		boolean pointerCompat = (flags & Flags.pointerCompat) != 0;
		return new Relation(type, pos, targetType, targetOffset, lemmaIndex + 1, targetMemberNum + 1, pointerCompat);
	}

	/**
	 * Build lexfile num (result does not matter in terms of output format length)
	 *
	 * @param synset synset
	 * @return lexfile num
	 */
	protected int buildLexfileNum(Synset synset)
	{
		String lexfile = synset.getLexfile();
		try
		{
			return Coder.codeLexFile(lexfile);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException(String.format("Lexfile '%s' in %s", lexfile, synset.getSynsetId()));
		}
	}

	/**
	 * Report
	 */
	public void report()
	{
		if (this.incompats.size() > 0)
		{
			for (Map.Entry<String, Integer> entry : incompats.entrySet())
			{
				Tracing.psErr.printf("[W] Incompatibilities '%s': %d%n", entry.getKey(), entry.getValue());
			}
			this.incompats.clear();
		}
	}
}
