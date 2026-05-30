/*
 * Copyright (c) 2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.oewntk.model.Sense
import java.io.PrintStream

/**
 * Grind tag count files
 */
class GrindTagCounts {

    /**
     * Grind tag count backward
     *
     * @param ps     print stream
     * @param senses senses
     */
    fun makeTagCountRev(ps: PrintStream, senses: Collection<Sense>) {
        val n = senses
            .filter { sense -> sense.tagCount != null }
            .sortedBy(Sense::senseKey)
            .onEach { sense ->
                val line = "${sense.senseKey} ${sense.lexIndex} ${sense.tagCount!!.count}"
                ps.println(line)
            }
            .count()
        Tracing.psInfo.println("Tag counts reverse: $n")
    }

    /**
     * Grind tag count forward
     *
     * @param ps      print stream
     * @param senses  senses
     */
    fun makeTagCount(ps: PrintStream, senses: Collection<Sense>) {
        val n = senses
            .asSequence()
            .filter { sense -> sense.tagCount != null }
            .map { sense -> "${sense.tagCount!!.count} ${sense.senseKey} ${sense.lexIndex}" }
            .sortedWith { l1: String, l2: String ->
                val fields1 = l1.split("\\s".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val fields2 = l2.split("\\s".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val field1 = fields1[0]
                val field2 = fields2[0]
                val i1 = field1.toInt()
                val i2 = field2.toInt()
                if (i1 == i2) {
                    val field21 = fields1[1]
                    val field22 = fields2[1]
                    field21.compareTo(field22)
                } else {
                    -i1.compareTo(i2)
                }
            }
            .onEach { ps.println(it) }
            .count()
        Tracing.psInfo.println("Tag counts: $n")
    }
}
