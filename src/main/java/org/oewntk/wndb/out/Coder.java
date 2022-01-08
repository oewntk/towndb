/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
 */

package org.oewntk.wndb.out;

import java.util.HashMap;
import java.util.Map;

/**
 * This class maps information to a documented code
 *
 * @author Bernard Bou
 */
public class Coder
{
	private Coder()
	{
	}

	private static final String IS_ENTAILED_PTR = "*^";

	private static final String IS_CAUSED_PTR = ">^";

	// R E L A T I O N

	static final String ANTONYM = "antonym";

	static final String HYPERNYM = "hypernym";

	static final String INSTANCE_HYPERNYM = "instance_hypernym";

	static final String HYPONYM = "hyponym";

	static final String INSTANCE_HYPONYM = "instance_hyponym";

	static final String HOLO_MEMBER = "holo_member";

	static final String HOLO_SUBSTANCE = "holo_substance";

	static final String HOLO_PART = "holo_part";

	static final String MERO_MEMBER = "mero_member";

	static final String MERO_SUBSTANCE = "mero_substance";

	static final String MERO_PART = "mero_part";

	static final String ATTRIBUTE = "attribute";

	static final String PERTAINYM = "pertainym";

	static final String DERIVATION = "derivation";

	static final String DOMAIN_TOPIC = "domain_topic";

	static final String HAS_DOMAIN_TOPIC = "has_domain_topic";

	static final String DOMAIN_REGION = "domain_region";

	static final String HAS_DOMAIN_REGION = "has_domain_region";

	static final String DOMAIN_USAGE = "exemplifies";

	static final String HAS_DOMAIN_USAGE = "is_exemplified_by";

	static final String ALSO = "also";

	static final String ENTAILS = "entails";

	static final String IS_ENTAILED = "is_entailed_by";

	static final String SIMILAR = "similar";

	static final String VERB_GROUP = "verb_group";

	static final String PARTICIPLE = "participle";

	static final String CAUSES = "causes";

	static final String IS_CAUSED = "is_caused_by";

	/**
	 * Code relation
	 *
	 * @param type          relation type
	 * @param pos           part-of-speech
	 * @param pointerCompat pointer compatibility
	 * @return code
	 */
	static String codeRelation(String type, char pos, boolean pointerCompat) throws CompatException
	{
		switch (pos)
		{
			case 'n':
			/*
			@formatter:off
			!    Antonym
			@    Hypernym
			@i    Instance Hypernym
			 ~    Hyponym
			 ~i    Instance Hyponym
			#m    Member holonym
			#s    Substance holonym
			#p    Part holonym
			%m    Member meronym
			%s    Substance meronym
			%p    Part meronym
			=    Attribute
			+    Derivationally related form
			;c    Domain of synset - TOPIC
			-c    Member of this domain - TOPIC
			;r    Domain of synset - REGION
			-r    Member of this domain - REGION
			;u    Domain of synset - USAGE
			-u    Member of this domain - USAGE
			@formatter:on
			*/
				//noinspection CommentedOutCode
				switch (type)
				{
					case ANTONYM:
						return "!";
					case HYPERNYM:
						return "@";
					case INSTANCE_HYPERNYM:
						return "@i";
					case HYPONYM:
						return "~";
					case INSTANCE_HYPONYM:
						return "~i";
					case HOLO_MEMBER:
						return "#m";
					case HOLO_SUBSTANCE:
						return "#s";
					case HOLO_PART:
						return "#p";
					case MERO_MEMBER:
						return "%m";
					case MERO_SUBSTANCE:
						return "%s";
					case MERO_PART:
						return "%p";
					case ATTRIBUTE:
						return "=";
					case PERTAINYM:
						return "\\"; // NOT DEFINED IN PWN
					case ALSO:
						return "^";  // NOT DEFINED IN PWN
					case DERIVATION:
						return "+";
					case DOMAIN_TOPIC:
						return ";c";
					case HAS_DOMAIN_TOPIC:
						return "-c";
					case DOMAIN_REGION:
						return ";r";
					case HAS_DOMAIN_REGION:
						return "-r";
					case DOMAIN_USAGE:
						return ";u";
					case HAS_DOMAIN_USAGE:
						return "-u";
					// case SIMILAR:
					//	if (Flags.POINTER_COMPAT)
					//	    throw new IllegalArgumentException(type + " for " + pos); // NOT DEFINED IN PWN
					//  return SIMILAR;
					default:
						break;
				}
				break;

			case 'v':
			/*
			@formatter:off
			!    Antonym
			@    Hypernym
			 ~    Hyponym
			*    Entailment
			>    Cause
			^    Also see
			$    Verb Group
			+    Derivationally related form
			;c    Domain of synset - TOPIC
			;r    Domain of synset - REGION
			;u    Domain of synset - USAGE
			@formatter:on
			*/
				switch (type)
				{
					case ANTONYM:
						return "!";
					case HYPERNYM:
						return "@";
					case HYPONYM:
						return "~";
					case ENTAILS:
						return "*";
					case CAUSES:
						return ">";
					case ALSO:
						return "^";
					case VERB_GROUP:
					case SIMILAR:
						return "$"; // verb group
					case DERIVATION:
						return "+";
					case DOMAIN_TOPIC:
						return ";c";
					case DOMAIN_REGION:
						return ";r";
					case DOMAIN_USAGE:
						return ";u";
					case IS_ENTAILED:
						if (pointerCompat)
						{
							throw new CompatException(new IllegalArgumentException(type)); // NOT DEFINED IN PWN
						}
						return IS_ENTAILED_PTR;
					case IS_CAUSED:
						if (pointerCompat)
						{
							throw new CompatException(new IllegalArgumentException(type)); // NOT DEFINED IN PWN
						}
						return IS_CAUSED_PTR;
					default:
						break;
				}
				break;

			case 'a':
			case 's':
			/*
			@formatter:off
			!    Antonym
			&    Similar to
			<    Participle of verb
			\    Pertainym (pertains to noun)
			=    Attribute
			^    Also see
			;c    Domain of synset - TOPIC
			;r    Domain of synset - REGION
			;u    Domain of synset - USAGE
			@formatter:on
			*/
				switch (type)
				{
					case ANTONYM:
						return "!";
					case SIMILAR:
						return "&";
					case PARTICIPLE:
						return "<";
					case PERTAINYM:
						return "\\";
					case ATTRIBUTE:
						return "=";
					case ALSO:
						return "^"; // NOT DEFINED IN PWN
					case DERIVATION:
						return "+"; // NOT DEFINED IN PWN

					case DOMAIN_TOPIC:
						return ";c";
					case DOMAIN_REGION:
						return ";r";
					case DOMAIN_USAGE:
						return ";u";

					case HAS_DOMAIN_TOPIC:
						return "-c"; //NS
					case HAS_DOMAIN_REGION:
						return "-r"; //NS
					case HAS_DOMAIN_USAGE:
						return "-u"; //NS
					default:
						break;
				}
				break;

			case 'r':
			/*
			 @formatter:off
			 !    Antonym
			 \    Derived from adjective
			 ;c    Domain of synset - TOPIC
			 ;r    Domain of synset - REGION
			 ;u    Domain of synset - USAGE
			 @formatter:on
			 */
				switch (type)
				{
					case ANTONYM:
						return "!";
					case PERTAINYM:
						return "\\"; // NS
					case ALSO:
						return "^";
					case DERIVATION:
						return "+";

					case DOMAIN_TOPIC:
						return ";c";
					case DOMAIN_REGION:
						return ";r";
					case DOMAIN_USAGE:
						return ";u";

					case HAS_DOMAIN_TOPIC:
						return "-c"; //NS
					case HAS_DOMAIN_REGION:
						return "-r"; //NS
					case HAS_DOMAIN_USAGE:
						return "-u"; //NS
					default:
						break;
				}
				break;

			default:
				break;
		}
		throw new IllegalArgumentException("pos=" + pos + " relType=" + type);
	}

	// V E R B F R A M E

	static final int LAST_COMPAT_VERBFRAME = 35;

	static final Map<String, Integer> FRAMEID_TO_NUM = new HashMap<>();

	static
	{
		FRAMEID_TO_NUM.put("vii", 1); // "Something ----s",
		FRAMEID_TO_NUM.put("via", 2); // "Somebody ----s",
		FRAMEID_TO_NUM.put("nonreferential", 3); // "It is ----ing",
		FRAMEID_TO_NUM.put("vii-pp", 4); // "Something is ----ing PP",
		FRAMEID_TO_NUM.put("vtii-adj", 5); // "Something ----s something Adjective/Noun",
		FRAMEID_TO_NUM.put("vii-adj", 6); // "Something ----s Adjective/Noun",
		FRAMEID_TO_NUM.put("via-adj", 7); // "Somebody ----s Adjective",
		FRAMEID_TO_NUM.put("vtai", 8); // "Somebody ----s something",
		FRAMEID_TO_NUM.put("vtaa", 9); // "Somebody ----s somebody",
		FRAMEID_TO_NUM.put("vtia", 10); // "Something ----s somebody",
		FRAMEID_TO_NUM.put("vtii", 11); // "Something ----s something",
		FRAMEID_TO_NUM.put("vii-to", 12); // "Something ----s to somebody",
		FRAMEID_TO_NUM.put("via-on-inanim", 13); // "Somebody ----s on something",
		FRAMEID_TO_NUM.put("ditransitive", 14); // "Somebody ----s somebody something",
		FRAMEID_TO_NUM.put("vtai-to", 15); // "Somebody ----s something to somebody",
		FRAMEID_TO_NUM.put("vtai-from", 16); // "Somebody ----s something from somebody",
		FRAMEID_TO_NUM.put("vtaa-with", 17); // "Somebody ----s somebody with something",
		FRAMEID_TO_NUM.put("vtaa-of", 18); // "Somebody ----s somebody of something",
		FRAMEID_TO_NUM.put("vtai-on", 19); // "Somebody ----s something on somebody",
		FRAMEID_TO_NUM.put("vtaa-pp", 20); // "Somebody ----s somebody PP",
		FRAMEID_TO_NUM.put("vtai-pp", 21); // "Somebody ----s something PP",
		FRAMEID_TO_NUM.put("via-pp", 22); // "Somebody ----s PP",
		FRAMEID_TO_NUM.put("vibody", 23); // "Somebody's (body part) ----s",
		FRAMEID_TO_NUM.put("vtaa-to-inf", 24); // "Somebody ----s somebody to INFINITIVE",
		FRAMEID_TO_NUM.put("vtaa-inf", 25); // "Somebody ----s somebody INFINITIVE",
		FRAMEID_TO_NUM.put("via-that", 26); // "Somebody ----s that CLAUSE",
		FRAMEID_TO_NUM.put("via-to", 27); // "Somebody ----s to somebody",
		FRAMEID_TO_NUM.put("via-to-inf", 28); // "Somebody ----s to INFINITIVE",
		FRAMEID_TO_NUM.put("via-whether-inf", 29); // "Somebody ----s whether INFINITIVE",
		FRAMEID_TO_NUM.put("vtaa-into-ger", 30); // "Somebody ----s somebody into V-ing something",
		FRAMEID_TO_NUM.put("vtai-with", 31); // "Somebody ----s something with something",
		FRAMEID_TO_NUM.put("via-inf", 32); // "Somebody ----s INFINITIVE",
		FRAMEID_TO_NUM.put("via-ger", 33); // "Somebody ----s VERB-ing",
		FRAMEID_TO_NUM.put("nonreferential-sent", 34); // "It ----s that CLAUSE",
		FRAMEID_TO_NUM.put("vii-inf", 35); // "Something ----s INFINITIVE",

		FRAMEID_TO_NUM.put("via-at", 36); // "Somebody ----s at something",
		FRAMEID_TO_NUM.put("via-for", 37); // "Somebody ----s for something",
		FRAMEID_TO_NUM.put("via-on-anim", 38); // "Somebody ----s on somebody",
		FRAMEID_TO_NUM.put("via-out-of", 39); // "Somebody ----s out of somebody",
	}

	/**
	 * Code verb frame
	 *
	 * @param frameid0        frame id
	 * @param verbFrameCompat verbFrame compatibility
	 * @return code
	 */
	static int codeFrameId(String frameid0, boolean verbFrameCompat) throws CompatException
	{
		String frameid = frameid0.trim();
		Integer n = FRAMEID_TO_NUM.get(frameid);
		if (n == null)
		{
			throw new IllegalArgumentException(frameid0);
		}
		if (verbFrameCompat && n > LAST_COMPAT_VERBFRAME)
		{
			throw new CompatException(new IllegalArgumentException(frameid0)); // NOT DEFINED IN PWN
		}
		return n;
	}

	// L E X F I L E

	static final Map<String, Integer> LEXFILE_TO_NUM = new HashMap<>();

	static
	{
		LEXFILE_TO_NUM.put("adj.all", 0);
		LEXFILE_TO_NUM.put("adj.pert", 1);
		LEXFILE_TO_NUM.put("adv.all", 2);
		LEXFILE_TO_NUM.put("noun.Tops", 3);
		LEXFILE_TO_NUM.put("noun.act", 4);
		LEXFILE_TO_NUM.put("noun.animal", 5);
		LEXFILE_TO_NUM.put("noun.artifact", 6);
		LEXFILE_TO_NUM.put("noun.attribute", 7);
		LEXFILE_TO_NUM.put("noun.body", 8);
		LEXFILE_TO_NUM.put("noun.cognition", 9);
		LEXFILE_TO_NUM.put("noun.communication", 10);
		LEXFILE_TO_NUM.put("noun.event", 11);
		LEXFILE_TO_NUM.put("noun.feeling", 12);
		LEXFILE_TO_NUM.put("noun.food", 13);
		LEXFILE_TO_NUM.put("noun.group", 14);
		LEXFILE_TO_NUM.put("noun.location", 15);
		LEXFILE_TO_NUM.put("noun.motive", 16);
		LEXFILE_TO_NUM.put("noun.object", 17);
		LEXFILE_TO_NUM.put("noun.person", 18);
		LEXFILE_TO_NUM.put("noun.phenomenon", 19);
		LEXFILE_TO_NUM.put("noun.plant", 20);
		LEXFILE_TO_NUM.put("noun.possession", 21);
		LEXFILE_TO_NUM.put("noun.process", 22);
		LEXFILE_TO_NUM.put("noun.quantity", 23);
		LEXFILE_TO_NUM.put("noun.relation", 24);
		LEXFILE_TO_NUM.put("noun.shape", 25);
		LEXFILE_TO_NUM.put("noun.state", 26);
		LEXFILE_TO_NUM.put("noun.substance", 27);
		LEXFILE_TO_NUM.put("noun.time", 28);
		LEXFILE_TO_NUM.put("verb.body", 29);
		LEXFILE_TO_NUM.put("verb.change", 30);
		LEXFILE_TO_NUM.put("verb.cognition", 31);
		LEXFILE_TO_NUM.put("verb.communication", 32);
		LEXFILE_TO_NUM.put("verb.competition", 33);
		LEXFILE_TO_NUM.put("verb.consumption", 34);
		LEXFILE_TO_NUM.put("verb.contact", 35);
		LEXFILE_TO_NUM.put("verb.creation", 36);
		LEXFILE_TO_NUM.put("verb.emotion", 37);
		LEXFILE_TO_NUM.put("verb.motion", 38);
		LEXFILE_TO_NUM.put("verb.perception", 39);
		LEXFILE_TO_NUM.put("verb.possession", 40);
		LEXFILE_TO_NUM.put("verb.social", 41);
		LEXFILE_TO_NUM.put("verb.stative", 42);
		LEXFILE_TO_NUM.put("verb.weather", 43);
		LEXFILE_TO_NUM.put("adj.ppl", 44);
	}

	/**
	 * Code lexfile
	 *
	 * @param name name of lex file
	 * @return code
	 */
	static int codeLexFile(String name)
	{
		return LEXFILE_TO_NUM.get(name);
	}
}
