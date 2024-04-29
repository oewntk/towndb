/*
 * Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-")2021. Bernard Bou.
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
     * @param ps         print stream
     * @param sensesById senses by id
     */
    fun makeTagCountRev(ps: PrintStream, sensesById: Map<String, Sense>) {
        var n: Long = 0
        for ((sensekey, sense) in sensesById) {
            val tagCount = sense.tagCount
            if (tagCount != null) {
                val line = String.format("%s %d %d", sensekey, sense.lexIndex, tagCount.count)
                ps.println(line)
                n++
            }
        }
        Tracing.psInfo.printf("Tag counts reverse: %d%n", n)
    }

    /**
     * Grind tag count forward
     *
     * @param ps         print stream
     * @param sensesById senses by id
     */
    fun makeTagCount(ps: PrintStream, sensesById: Map<String, Sense>) {
        val lines: MutableList<String> = ArrayList()
        for ((sensekey, sense) in sensesById) {
            val tagCount = sense.tagCount
            if (tagCount != null) {
                val line = String.format("%d %s %d", tagCount.count, sensekey, sense.lexIndex)
                lines.add(line)
            }
        }
        lines.sortWith { l1, l2 ->
            val field1 = l1.split("\\s".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            val field2 = l2.split("\\s".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            val i1 = field1.toInt()
            val i2 = field2.toInt()
            -i1.compareTo(i2)
        }
        var n: Long = 0
        for (line in lines) {
            ps.println(line)
            n++
        }
        Tracing.psInfo.printf("Tag counts: %d%n", n)
    }
}
