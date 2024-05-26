/*
 * Copyright (c) 2024. Bernard Bou.
 */

package org.oewntk.wndb.out

import java.util.function.Consumer

fun <T> Sequence<T>.toSetWarnDuplicates(onDuplicate: Consumer<T>): Set<T> {
    val it = iterator()
    if (!it.hasNext())
        return emptySet()
    var element = it.next()
    if (!it.hasNext())
        return setOf(element)
    val dst = LinkedHashSet<T>()
    dst.add(element)
    while (it.hasNext()) {
        element = it.next()
        val wasThere = !dst.add(element)
        if (wasThere) {
            onDuplicate.accept(element)
        }
    }
    return dst
}
