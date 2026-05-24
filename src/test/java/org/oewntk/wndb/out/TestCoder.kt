/*
 * Copyright (c) 2021-2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.junit.Test
import org.oewntk.model.PartOfSpeech
import org.oewntk.model.PartOfSpeechImpl
import org.oewntk.wndb.out.Coder.codeRelation
import java.util.*

class TestCoder {

    @Test
    fun testCoderCompat() {
        testCoder(true)
    }

    @Test
    fun testCoderNoCompat() {
        testCoder(true)
    }

    @Suppress("SameParameterValue")
    private fun testCoder(pointerCompat: Boolean) {
        val allRelations: MutableMap<PartOfSpeech, MutableSet<String>> = EnumMap(PartOfSpeechImpl::class.java)
        val nSet = allRelations.computeIfAbsent(PartOfSpeech.N) { HashSet() }
        nSet.addAll(
            listOf(
                Coder.ANTONYM,
                Coder.HYPERNYM,
                Coder.INSTANCE_HYPERNYM,
                Coder.HYPONYM,
                Coder.INSTANCE_HYPONYM,
                Coder.HOLO_MEMBER,
                Coder.HOLO_SUBSTANCE,
                Coder.HOLO_PART,
                Coder.MERO_MEMBER,
                Coder.MERO_SUBSTANCE,
                Coder.MERO_PART,
                Coder.ATTRIBUTE,
                Coder.PERTAINYM,
                Coder.DERIVATION,
                Coder.DOMAIN_TOPIC,
                Coder.HAS_DOMAIN_TOPIC,
                Coder.DOMAIN_REGION,
                Coder.HAS_DOMAIN_REGION,
                Coder.DOMAIN_USAGE,
                Coder.HAS_DOMAIN_USAGE,
                Coder.COLLOCATION,
            )
        )
        val vSet = allRelations.computeIfAbsent(PartOfSpeech.V) { HashSet() }
        vSet.addAll(
            listOf(
                Coder.ANTONYM,
                Coder.HYPERNYM,
                Coder.HYPONYM,
                Coder.ENTAILS,
                Coder.IS_ENTAILED,
                Coder.CAUSES,
                Coder.IS_CAUSED,
                Coder.ALSO,
                Coder.VERB_GROUP,
                Coder.DERIVATION,
                Coder.DOMAIN_TOPIC,
                Coder.DOMAIN_REGION,
                Coder.DOMAIN_USAGE,
                Coder.COLLOCATION,
            )
        )
        val aSet = allRelations.computeIfAbsent(PartOfSpeech.A) { HashSet() }
        aSet.addAll(
            listOf(
                Coder.ANTONYM,
                Coder.SIMILAR,
                Coder.PARTICIPLE,
                Coder.PERTAINYM,
                Coder.ATTRIBUTE,
                Coder.ALSO,
                Coder.DERIVATION,
                Coder.DOMAIN_TOPIC,
                Coder.DOMAIN_REGION,
                Coder.DOMAIN_USAGE,
                Coder.HAS_DOMAIN_TOPIC,
                Coder.HAS_DOMAIN_REGION,
                Coder.HAS_DOMAIN_USAGE,
                Coder.COLLOCATION,
            )
        )
        val rSet = allRelations.computeIfAbsent(PartOfSpeech.R) { HashSet() }
        rSet.addAll(
            listOf(
                Coder.ANTONYM,
                Coder.PERTAINYM,
                Coder.DERIVATION,
                Coder.DOMAIN_TOPIC,
                Coder.DOMAIN_REGION,
                Coder.DOMAIN_USAGE,
                Coder.HAS_DOMAIN_TOPIC,
                Coder.HAS_DOMAIN_REGION,
                Coder.HAS_DOMAIN_USAGE,
                Coder.COLLOCATION,
            )
        )

        val allPointers: MutableSet<String> = TreeSet()
        val toRelations: MutableMap<PartOfSpeech, MutableMap<String, String>> = EnumMap(PartOfSpeechImpl::class.java)
        PartOfSpeech.entries.forEach { pos ->
            allRelations[pos]!!.forEach {
                try {
                    val pointer: String = codeRelation(it, pos, pointerCompat)
                    allPointers.add(pointer)
                    val pointerToRelation = toRelations.computeIfAbsent(pos) { HashMap() }
                    pointerToRelation[pointer] = it
                } catch (e: CompatException) {
                    Tracing.psErr.println(e.cause!!.message)
                } catch (e: IllegalArgumentException) {
                    Tracing.psErr.println(it + " for " + pos + " " + e.cause!!.message)
                }
            }
        }
        allPointers.forEach { pointer ->
            Tracing.psInfo.println("${String.format("%-2s", pointer)}\t")
            PartOfSpeech.entries.forEach { pos ->
                val relation = toRelations[pos]!![pointer]
                if (relation != null) {
                    Tracing.psInfo.print("$pos:$relation ")
                }
            }
            Tracing.psInfo.println()
        }
    }
}
