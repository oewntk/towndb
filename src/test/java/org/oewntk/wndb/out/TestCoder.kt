/*
 * Copyright (c) 2021-2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.junit.Test
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

    private fun testCoder(pointerCompat: Boolean) {
        val allRelations: MutableMap<Char, MutableSet<String>> = HashMap()
        val nSet = allRelations.computeIfAbsent('n') { HashSet() }
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
                Coder.HAS_DOMAIN_USAGE
            )
        )
        val vSet = allRelations.computeIfAbsent('v') { HashSet() }
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
                Coder.DOMAIN_USAGE
            )
        )
        val aSet = allRelations.computeIfAbsent('a') { HashSet() }
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
                Coder.HAS_DOMAIN_USAGE
            )
        )
        val rSet = allRelations.computeIfAbsent('r') { HashSet() }
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
                Coder.HAS_DOMAIN_USAGE
            )
        )

        val allPointers: MutableSet<String> = TreeSet()
        val toRelations: MutableMap<Char, MutableMap<String, String>> = HashMap()
        for (pos in mutableListOf('n', 'v', 'a', 'r')) {
            for (relation in allRelations[pos]!!) {
                var pointer: String
                try {
                    pointer = codeRelation(relation, pos, pointerCompat)
                } catch (e: CompatException) {
                    Tracing.psErr.println(e.cause!!.message)
                    continue
                } catch (e: IllegalArgumentException) {
                    Tracing.psErr.println(relation + " for " + pos + " " + e.cause!!.message)
                    continue
                }
                allPointers.add(pointer)
                val pointerToRelation = toRelations.computeIfAbsent(pos) { HashMap() }
                pointerToRelation[pointer] = relation
            }
        }
        for (pointer in allPointers) {
            Tracing.psInfo.printf("%-2s\t", pointer)
            for (pos in mutableListOf('n', 'v', 'a', 'r')) {
                val relation = toRelations[pos]!![pointer]
                if (relation != null) {
                    Tracing.psInfo.printf("%s:%s  ", pos, relation)
                }
            }
            Tracing.psInfo.println()
        }
    }
}
