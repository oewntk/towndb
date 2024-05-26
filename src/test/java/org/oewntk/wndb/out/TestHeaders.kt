/*
 * Copyright (c) 2021-2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import org.junit.Assert
import org.junit.Test

class TestHeaders {

    @Test
    fun testHeaders() {
        val pwnHeader = Formatter.PRINCETON_HEADER.length
        val oewnHeader = Formatter.OEWN_HEADER.length
        println("PWN $pwnHeader")
        println("OEWN $oewnHeader")
        Assert.assertEquals(pwnHeader.toLong(), oewnHeader.toLong())
    }
}
