/*
 * Copyright (c) 2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.PosId

/**
 * This class maps information to a documented code
 *
 * @author Bernard Bou
 */
object Coder {

    private const val IS_ENTAILED_PTR = "*^"

    private const val IS_CAUSED_PTR = ">^"

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

    /**
     * Code relation
     *
     * @param type          relation type
     * @param pos           part-of-speech
     * @param pointerCompat pointer compatibility
     * @return code
     */
    @Throws(CompatException::class)
    fun codeRelation(type: String, pos: PosId, pointerCompat: Boolean): String {
        when (pos) {
            'n'      -> when (type) {
                ANTONYM           -> return "!"
                HYPERNYM          -> return "@"
                INSTANCE_HYPERNYM -> return "@i"
                HYPONYM           -> return "~"
                INSTANCE_HYPONYM  -> return "~i"
                HOLO_MEMBER       -> return "#m"
                HOLO_SUBSTANCE    -> return "#s"
                HOLO_PART         -> return "#p"
                MERO_MEMBER       -> return "%m"
                MERO_SUBSTANCE    -> return "%s"
                MERO_PART         -> return "%p"
                ATTRIBUTE         -> return "="
                PERTAINYM         -> return "\\" // NOT DEFINED IN PWN
                ALSO              -> return "^" // NOT DEFINED IN PWN
                DERIVATION        -> return "+"
                DOMAIN_TOPIC      -> return ";c"
                HAS_DOMAIN_TOPIC  -> return "-c"
                DOMAIN_REGION     -> return ";r"
                HAS_DOMAIN_REGION -> return "-r"
                DOMAIN_USAGE      -> return ";u"
                HAS_DOMAIN_USAGE  -> return "-u"
                else              -> {}
            }

            'v'      -> when (type) {
                ANTONYM             -> return "!"
                HYPERNYM            -> return "@"
                HYPONYM             -> return "~"
                ENTAILS             -> return "*"
                CAUSES              -> return ">"
                ALSO                -> return "^"
                VERB_GROUP, SIMILAR -> return "$" // verb group
                DERIVATION          -> return "+"
                DOMAIN_TOPIC        -> return ";c"
                DOMAIN_REGION       -> return ";r"
                DOMAIN_USAGE        -> return ";u"
                IS_ENTAILED         -> {
                    if (pointerCompat) {
                        throw CompatException(IllegalArgumentException(type)) // NOT DEFINED IN PWN
                    }
                    return IS_ENTAILED_PTR
                }

                IS_CAUSED           -> {
                    if (pointerCompat) {
                        throw CompatException(IllegalArgumentException(type)) // NOT DEFINED IN PWN
                    }
                    return IS_CAUSED_PTR
                }

                else                -> {}
            }

            'a', 's' -> when (type) {
                ANTONYM           -> return "!"
                SIMILAR           -> return "&"
                PARTICIPLE        -> return "<"
                PERTAINYM         -> return "\\"
                ATTRIBUTE         -> return "="
                ALSO              -> return "^" // NOT DEFINED IN PWN
                DERIVATION        -> return "+" // NOT DEFINED IN PWN

                DOMAIN_TOPIC      -> return ";c"
                DOMAIN_REGION     -> return ";r"
                DOMAIN_USAGE      -> return ";u"

                HAS_DOMAIN_TOPIC  -> return "-c" // NS
                HAS_DOMAIN_REGION -> return "-r" // NS
                HAS_DOMAIN_USAGE  -> return "-u" // NS
                else              -> {}
            }

            'r'      -> when (type) {
                ANTONYM           -> return "!"
                PERTAINYM         -> return "\\" // NS
                ALSO              -> return "^"
                DERIVATION        -> return "+"

                DOMAIN_TOPIC      -> return ";c"
                DOMAIN_REGION     -> return ";r"
                DOMAIN_USAGE      -> return ";u"

                HAS_DOMAIN_TOPIC  -> return "-c" // NS
                HAS_DOMAIN_REGION -> return "-r" // NS
                HAS_DOMAIN_USAGE  -> return "-u" // NS
                else              -> {}
            }

            else     -> {}
        }
        throw IllegalArgumentException("pos=$pos relType=$type")
    }

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
        "adj.all" to 0,
        "adj.pert" to 1,
        "adv.all" to 2,
        "noun.Tops" to 3,
        "noun.act" to 4,
        "noun.animal" to 5,
        "noun.artifact" to 6,
        "noun.attribute" to 7,
        "noun.body" to 8,
        "noun.cognition" to 9,
        "noun.communication" to 10,
        "noun.event" to 11,
        "noun.feeling" to 12,
        "noun.food" to 13,
        "noun.group" to 14,
        "noun.location" to 15,
        "noun.motive" to 16,
        "noun.object" to 17,
        "noun.person" to 18,
        "noun.phenomenon" to 19,
        "noun.plant" to 20,
        "noun.possession" to 21,
        "noun.process" to 22,
        "noun.quantity" to 23,
        "noun.relation" to 24,
        "noun.shape" to 25,
        "noun.state" to 26,
        "noun.substance" to 27,
        "noun.time" to 28,
        "verb.body" to 29,
        "verb.change" to 30,
        "verb.cognition" to 31,
        "verb.communication" to 32,
        "verb.competition" to 33,
        "verb.consumption" to 34,
        "verb.contact" to 35,
        "verb.creation" to 36,
        "verb.emotion" to 37,
        "verb.motion" to 38,
        "verb.perception" to 39,
        "verb.possession" to 40,
        "verb.social" to 41,
        "verb.stative" to 42,
        "verb.weather" to 43,
        "adj.ppl" to 44,
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
