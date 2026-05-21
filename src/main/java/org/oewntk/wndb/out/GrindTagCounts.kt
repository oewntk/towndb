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
                val field1 = l1.split("\\s".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                val field2 = l2.split("\\s".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                val i1 = field1.toInt()
                val i2 = field2.toInt()
                -i1.compareTo(i2)
            }
            .onEach { ps.println(it) }
            .count()
        Tracing.psInfo.println("Tag counts: $n")
    }
}
