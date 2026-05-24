/*
 * Copyright (c) 2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.PartOfSpeech
import org.oewntk.model.Relation
import org.oewntk.model.SynsetType

/**
 * This class maps information to a documented code
 *
 * @author Bernard Bou
 */
object Coder {

    private const val IS_ENTAILED_PTR = "*^"

    private const val IS_CAUSED_PTR = ">^"

    private const val COLLOCATION_PTR = "`"

    // R E L A T I O N

    const val ANTONYM: String = "antonym"

    const val HYPERNYM: String = "hypernym"

    const val INSTANCE_HYPERNYM: String = "instance_hypernym"

    const val HYPONYM: String = "hyponym"

    const val INSTANCE_HYPONYM: String = "instance_hyponym"

    const val HOLO_MEMBER: String = "holo_member"

    const val HOLO_SUBSTANCE: String = "holo_substance"

    const val HOLO_PART: String = "holo_part"

    const val MERO_MEMBER: String = "mero_member"

    const val MERO_SUBSTANCE: String = "mero_substance"

    const val MERO_PART: String = "mero_part"

    const val ATTRIBUTE: String = "attribute"

    const val PERTAINYM: String = "pertainym"

    const val DERIVATION: String = "derivation"

    const val DOMAIN_TOPIC: String = "domain_topic"

    const val HAS_DOMAIN_TOPIC: String = "has_domain_topic"

    const val DOMAIN_REGION: String = "domain_region"

    const val HAS_DOMAIN_REGION: String = "has_domain_region"

    const val DOMAIN_USAGE: String = "exemplifies"

    const val HAS_DOMAIN_USAGE: String = "is_exemplified_by"

    const val ALSO: String = "also"

    const val ENTAILS: String = "entails"

    const val IS_ENTAILED: String = "is_entailed_by"

    const val SIMILAR: String = "similar"

    const val VERB_GROUP: String = "verb_group"

    const val PARTICIPLE: String = "participle"

    const val CAUSES: String = "causes"

    const val IS_CAUSED: String = "is_caused_by"

    const val COLLOCATION: String = "collocation"

    /**
     * Code relation
     *
     * @param rel           relation type
     * @param pos           part-of-speech
     * @param pointerCompat pointer compatibility
     * @return code
     */
    @Throws(CompatException::class)
    fun codeRelation(rel: Relation, pos: PartOfSpeech, pointerCompat: Boolean): String {
        when (pos) {
            PartOfSpeech.N -> when (rel) {
                ANTONYM -> return "!"
                HYPERNYM -> return "@"
                INSTANCE_HYPERNYM -> return "@i"
                HYPONYM -> return "~"
                INSTANCE_HYPONYM -> return "~i"
                HOLO_MEMBER -> return "#m"
                HOLO_SUBSTANCE -> return "#s"
                HOLO_PART -> return "#p"
                MERO_MEMBER -> return "%m"
                MERO_SUBSTANCE -> return "%s"
                MERO_PART -> return "%p"
                ATTRIBUTE -> return "="
                PERTAINYM -> return "\\" // NOT DEFINED IN PWN
                ALSO -> return "^" // NOT DEFINED IN PWN
                DERIVATION -> return "+"
                DOMAIN_TOPIC -> return ";c"
                HAS_DOMAIN_TOPIC -> return "-c"
                DOMAIN_REGION -> return ";r"
                HAS_DOMAIN_REGION -> return "-r"
                DOMAIN_USAGE -> return ";u"
                HAS_DOMAIN_USAGE -> return "-u"
                COLLOCATION -> {
                    if (pointerCompat) {
                        throw CompatException(IllegalArgumentException(rel)) // NOT DEFINED IN PWN
                    }
                    return COLLOCATION_PTR
                }

                else -> {}
            }

            PartOfSpeech.V -> when (rel) {
                ANTONYM -> return "!"
                HYPERNYM -> return "@"
                HYPONYM -> return "~"
                ENTAILS -> return "*"
                CAUSES -> return ">"
                ALSO -> return "^"
                VERB_GROUP, SIMILAR -> return "$" // verb group
                DERIVATION -> return "+"
                DOMAIN_TOPIC -> return ";c"
                DOMAIN_REGION -> return ";r"
                DOMAIN_USAGE -> return ";u"
                IS_ENTAILED -> {
                    if (pointerCompat) {
                        throw CompatException(IllegalArgumentException(rel)) // NOT DEFINED IN PWN
                    }
                    return IS_ENTAILED_PTR
                }

                IS_CAUSED -> {
                    if (pointerCompat) {
                        throw CompatException(IllegalArgumentException(rel)) // NOT DEFINED IN PWN
                    }
                    return IS_CAUSED_PTR
                }

                COLLOCATION -> {
                    if (pointerCompat) {
                        throw CompatException(IllegalArgumentException(rel)) // NOT DEFINED IN PWN
                    }
                    return COLLOCATION_PTR
                }

                else -> {}
            }

            PartOfSpeech.A -> when (rel) {
                ANTONYM -> return "!"
                SIMILAR -> return "&"
                PARTICIPLE -> return "<"
                PERTAINYM -> return "\\"
                ATTRIBUTE -> return "="
                ALSO -> return "^" // NOT DEFINED IN PWN
                DERIVATION -> return "+" // NOT DEFINED IN PWN
                DOMAIN_TOPIC -> return ";c"
                DOMAIN_REGION -> return ";r"
                DOMAIN_USAGE -> return ";u"
                HAS_DOMAIN_TOPIC -> return "-c" // NS
                HAS_DOMAIN_REGION -> return "-r" // NS
                HAS_DOMAIN_USAGE -> return "-u" // NS
                COLLOCATION -> {
                    if (pointerCompat) {
                        throw CompatException(IllegalArgumentException(rel)) // NOT DEFINED IN PWN
                    }
                    return COLLOCATION_PTR
                }

                else -> {}
            }

            PartOfSpeech.R -> when (rel) {
                ANTONYM -> return "!"
                PERTAINYM -> return "\\" // NS
                ALSO -> return "^"
                DERIVATION -> return "+"
                DOMAIN_TOPIC -> return ";c"
                DOMAIN_REGION -> return ";r"
                DOMAIN_USAGE -> return ";u"
                HAS_DOMAIN_TOPIC -> return "-c" // NS
                HAS_DOMAIN_REGION -> return "-r" // NS
                HAS_DOMAIN_USAGE -> return "-u" // NS
                COLLOCATION -> {
                    if (pointerCompat) {
                        throw CompatException(IllegalArgumentException(rel)) // NOT DEFINED IN PWN
                    }
                    return COLLOCATION_PTR
                }

                else -> {}
            }

            else -> {}
        }
        throw IllegalArgumentException("category=$pos relType=$rel")
    }

    val relationOrder = mapOf(
        HYPERNYM to 1,
        HYPONYM to 2,
        INSTANCE_HYPERNYM to 3,
        INSTANCE_HYPONYM to 4,
        HOLO_PART to 11,
        MERO_PART to 12,
        HOLO_MEMBER to 13,
        MERO_MEMBER to 14,
        HOLO_SUBSTANCE to 15,
        MERO_SUBSTANCE to 16,
        ENTAILS to 21,
        IS_ENTAILED to 22,
        CAUSES to 23,
        IS_CAUSED to 24,
        ANTONYM to 30,
        SIMILAR to 40,
        ALSO to 50,
        ATTRIBUTE to 60,
        VERB_GROUP to 70,
        PARTICIPLE to 71,
        PERTAINYM to 80,
        DERIVATION to 81,
        DOMAIN_TOPIC to 91,
        HAS_DOMAIN_TOPIC to 92,
        DOMAIN_REGION to 93,
        HAS_DOMAIN_REGION to 94,
        DOMAIN_USAGE to 95,  // DOMAIN USAGE == EXEMPLIFIES
        HAS_DOMAIN_USAGE to 96,  // DOMAIN MEMBER USAGE == HAS DOMAIN USAGE == IS_EXEMPLIFIED_BY
        // DOMAIN to 97,
        // MEMBER to 98,
        // OTHER to 99,

        // STATE to 100,
        // RESULT to 101,
        // EVENT to 102,
        // PROPERTY to 110,
        // LOCATION to 120,
        // DESTINATION to 121,
        // AGENT to 130,
        // UNDERGOER to 131,
        // USES to 140,
        // INSTRUMENT to 141,
        // BY_MEANS_OF to 142,
        // MATERIAL to 150,
        // VEHICLE to 160,  //,
        // BODY_PART to 170,
        COLLOCATION to 200,
    )

    val relationComparator: Comparator<Relation> = Comparator.comparingInt { relation -> relationOrder[relation]!! }

    // V E R B F R A M E

    private const val LAST_COMPAT_VERBFRAME: Int = 35

    private val FRAMEID_TO_NUM = mapOf(
        "vii" to 1, // "Something ----s",
        "via" to 2, // "Somebody ----s",
        "nonreferential" to 3, // "It is ----ing",
        "vii-pp" to 4, // "Something is ----ing PP",
        "vtii-adj" to 5, // "Something ----s something Adjective/Noun",
        "vii-adj" to 6, // "Something ----s Adjective/Noun",
        "via-adj" to 7, // "Somebody ----s Adjective",
        "vtai" to 8, // "Somebody ----s something",
        "vtaa" to 9, // "Somebody ----s somebody",
        "vtia" to 10, // "Something ----s somebody",
        "vtii" to 11, // "Something ----s something",
        "vii-to" to 12, // "Something ----s to somebody",
        "via-on-inanim" to 13, // "Somebody ----s on something",
        "ditransitive" to 14, // "Somebody ----s somebody something",
        "vtai-to" to 15, // "Somebody ----s something to somebody",
        "vtai-from" to 16, // "Somebody ----s something from somebody",
        "vtaa-with" to 17, // "Somebody ----s somebody with something",
        "vtaa-of" to 18, // "Somebody ----s somebody of something",
        "vtai-on" to 19, // "Somebody ----s something on somebody",
        "vtaa-pp" to 20, // "Somebody ----s somebody PP",
        "vtai-pp" to 21, // "Somebody ----s something PP",
        "via-pp" to 22, // "Somebody ----s PP",
        "vibody" to 23, // "Somebody's (body part) ----s",
        "vtaa-to-inf" to 24, // "Somebody ----s somebody to INFINITIVE",
        "vtaa-inf" to 25, // "Somebody ----s somebody INFINITIVE",
        "via-that" to 26, // "Somebody ----s that CLAUSE",
        "via-to" to 27, // "Somebody ----s to somebody",
        "via-to-inf" to 28, // "Somebody ----s to INFINITIVE",
        "via-whether-inf" to 29, // "Somebody ----s whether INFINITIVE",
        "vtaa-into-ger" to 30, // "Somebody ----s somebody into V-ing something",
        "vtai-with" to 31, // "Somebody ----s something with something",
        "via-inf" to 32, // "Somebody ----s INFINITIVE",
        "via-ger" to 33, // "Somebody ----s VERB-ing",
        "nonreferential-sent" to 34, // "It ----s that CLAUSE",
        "vii-inf" to 35, // "Something ----s INFINITIVE",

        "via-at" to 36, // "Somebody ----s at something",
        "via-for" to 37, // "Somebody ----s for something",
        "via-on-anim" to 38, // "Somebody ----s on somebody",
        "via-out-of" to 39, // "Somebody ----s out of somebody",
    )

    /**
     * Code verb frame
     *
     * @param frameid0        frame id
     * @param verbFrameCompat verbFrame compatibility
     * @return code
     */
    @Throws(CompatException::class)
    fun codeFrameId(frameid0: String, verbFrameCompat: Boolean): Int {
        val frameid = frameid0.trim { it <= ' ' }
        val n = FRAMEID_TO_NUM[frameid] ?: throw IllegalArgumentException(frameid0)
        if (verbFrameCompat && n > LAST_COMPAT_VERBFRAME) {
            throw CompatException(IllegalArgumentException(frameid0)) // NOT DEFINED IN PWN
        }
        return n
    }

    // L E X F I L E

    val LEXFILE_TO_NUM = mapOf(
        "${PartOfSpeech.A.fullName}.all" to 0,
        "${PartOfSpeech.A.fullName}.pert" to 1,
        "${PartOfSpeech.R.fullName}.all" to 2,
        "${PartOfSpeech.N.fullName}.Tops" to 3,
        "${PartOfSpeech.N.fullName}.act" to 4,
        "${PartOfSpeech.N.fullName}.animal" to 5,
        "${PartOfSpeech.N.fullName}.artifact" to 6,
        "${PartOfSpeech.N.fullName}.attribute" to 7,
        "${PartOfSpeech.N.fullName}.body" to 8,
        "${PartOfSpeech.N.fullName}.cognition" to 9,
        "${PartOfSpeech.N.fullName}.communication" to 10,
        "${PartOfSpeech.N.fullName}.event" to 11,
        "${PartOfSpeech.N.fullName}.feeling" to 12,
        "${PartOfSpeech.N.fullName}.food" to 13,
        "${PartOfSpeech.N.fullName}.group" to 14,
        "${PartOfSpeech.N.fullName}.location" to 15,
        "${PartOfSpeech.N.fullName}.motive" to 16,
        "${PartOfSpeech.N.fullName}.object" to 17,
        "${PartOfSpeech.N.fullName}.person" to 18,
        "${PartOfSpeech.N.fullName}.phenomenon" to 19,
        "${PartOfSpeech.N.fullName}.plant" to 20,
        "${PartOfSpeech.N.fullName}.possession" to 21,
        "${PartOfSpeech.N.fullName}.process" to 22,
        "${PartOfSpeech.N.fullName}.quantity" to 23,
        "${PartOfSpeech.N.fullName}.relation" to 24,
        "${PartOfSpeech.N.fullName}.shape" to 25,
        "${PartOfSpeech.N.fullName}.state" to 26,
        "${PartOfSpeech.N.fullName}.substance" to 27,
        "${PartOfSpeech.N.fullName}.time" to 28,
        "${PartOfSpeech.V.fullName}.body" to 29,
        "${PartOfSpeech.V.fullName}.change" to 30,
        "${PartOfSpeech.V.fullName}.cognition" to 31,
        "${PartOfSpeech.V.fullName}.communication" to 32,
        "${PartOfSpeech.V.fullName}.competition" to 33,
        "${PartOfSpeech.V.fullName}.consumption" to 34,
        "${PartOfSpeech.V.fullName}.contact" to 35,
        "${PartOfSpeech.V.fullName}.creation" to 36,
        "${PartOfSpeech.V.fullName}.emotion" to 37,
        "${PartOfSpeech.V.fullName}.motion" to 38,
        "${PartOfSpeech.V.fullName}.perception" to 39,
        "${PartOfSpeech.V.fullName}.possession" to 40,
        "${PartOfSpeech.V.fullName}.social" to 41,
        "${PartOfSpeech.V.fullName}.stative" to 42,
        "${PartOfSpeech.V.fullName}.weather" to 43,
        "${PartOfSpeech.A.fullName}.ppl" to 44,
    )

    /**
     * Code lexfile
     *
     * @param name name of lex file
     * @return code
     */
    fun codeLexFile(name: String): Int {
        return LEXFILE_TO_NUM[name]!!
    }
}
