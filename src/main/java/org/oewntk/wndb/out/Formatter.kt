/*
 * Copyright (c) 2024. Bernard Bou.
 */
package org.oewntk.wndb.out

import java.time.LocalDate

/**
 * Format utilities
 *
 * @author Bernard Bou
 */
object Formatter {

    fun offsetFormat(offset: Long): String {
        return String.format("%08d", offset)
    }

    fun lexidFormat(lexId: Int, lexIdCompat: Boolean = false): String {
        return String.format(if (lexIdCompat) "%1X" else "%X", lexId)
    }

    fun intFormat2(int: Int): String {
        return String.format("%02d", int)
    }

    fun intFormat3(int: Int): String {
        return String.format("%03d", int)
    }

    fun intFormatHex2x(int: Int): String {
        return String.format("%02x", int)
    }

    fun intFormatHex2X(int: Int): String {
        return String.format("%02X", int)
    }

    /**
     * Escape string
     *
     * @param item string to escape
     * @return escaped string
     */
    fun escape(item: String): String {
        return item.replace(' ', '_')
    }

    /**
     * Join items, prefix with count
     *
     * @param T           type of item
     * @param countFormat format of count field
     * @param transform   string function to represent item
     * @return joined string representation of items preceded by count
     */
    fun <T> Collection<T>.joinToStringWithCount(separator: CharSequence = " ", countSeparator: String = " ", countFormat: (Int) -> String, transform: ((T) -> String)? = null): String {
        return this.joinToString(prefix = countFormat(size) + if (isNotEmpty()) countSeparator else "", separator = separator, transform = transform)
    }

    /**
     * Header of data files
     */
    const val PRINCETON_HEADER: String =
        "  1 This software and database is being provided to you, the LICENSEE, by  \n" +
                "  2 Princeton University under the following license.  By obtaining, using  \n" +
                "  3 and/or copying this software and database, you agree that you have  \n" +
                "  4 read, understood, and will comply with these terms and conditions.:  \n" +
                "  5   \n" +
                "  6 Permission to use, copy, modify and distribute this software and  \n" +
                "  7 database and its documentation for any purpose and without fee or  \n" +
                "  8 royalty is hereby granted, provided that you agree to comply with  \n" +
                "  9 the following copyright notice and statements, including the disclaimer,  \n" +
                "  10 and that the same appear on ALL copies of the software, database and  \n" +
                "  11 documentation, including modifications that you make for internal  \n" +
                "  12 use or for distribution.  \n" +
                "  13   \n" +
                "  14 WordNet 3.1 Copyright 2011 by Princeton University.  All rights reserved.  \n" +
                "  15   \n" +
                "  16 THIS SOFTWARE AND DATABASE IS PROVIDED \"AS IS\" AND PRINCETON  \n" +
                "  17 UNIVERSITY MAKES NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR  \n" +
                "  18 IMPLIED.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, PRINCETON  \n" +
                "  19 UNIVERSITY MAKES NO REPRESENTATIONS OR WARRANTIES OF MERCHANT-  \n" +
                "  20 ABILITY OR FITNESS FOR ANY PARTICULAR PURPOSE OR THAT THE USE  \n" +
                "  21 OF THE LICENSED SOFTWARE, DATABASE OR DOCUMENTATION WILL NOT  \n" +
                "  22 INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR  \n" +
                "  23 OTHER RIGHTS.  \n" +
                "  24   \n" +
                "  25 The name of Princeton University or Princeton may not be used in  \n" +
                "  26 advertising or publicity pertaining to distribution of the software  \n" +
                "  27 and/or database.  Title to copyright in this software, database and  \n" +
                "  28 any associated documentation shall at all times remain with  \n" +
                "  29 Princeton University and LICENSEE agrees to preserve same.  \n"

    private val OEWN_YEAR: String = LocalDate.now().year.toString()

    val OEWN_HEADER: String =
        "  1 This software and database is being provided to you, the LICENSEE, by  \n" +
                "  2 the Open English Wordnet team under the Creative Commons Attribution 4.0  \n" +
                "  3 International License (CC-BY 4.0).  \n" +
                "  4 Open English Wordnet $OEWN_YEAR Copyright 2021 by the Open English Wordnet team.  \n" +
                "  5 \n" +
                "  6 Permission to use, copy, modify and distribute this software and  \n" +
                "  7 database and its documentation for any purpose and without fee or  \n" +
                "  8 royalty is hereby granted, provided that you agree to comply with  \n" +
                "  9 the following copyright notice and statements, including the disclaimer,  \n" +
                "  10 and that the same appear on ALL copies of the software, database and  \n" +
                "  11 documentation, including modifications that you make for internal  \n" +
                "  12 use or for distribution.  \n" +
                "  13 \n" +
                "  14 WordNet 3.1 Copyright 2011 by Princeton University.  All rights reserved.  \n" +
                "  15 THIS SOFTWARE AND DATABASE IS PROVIDED \"AS IS\" AND PRINCETON  \n" +
                "  16 UNIVERSITY MAKES NO REPRESENTATIONS OR WARRANTIES, EXPRESS OR  \n" +
                "  17 IMPLIED.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, PRINCETON  \n" +
                "  18 UNIVERSITY MAKES NO REPRESENTATIONS OR WARRANTIES OF MERCHANT-  \n" +
                "  19 ABILITY OR FITNESS FOR ANY PARTICULAR PURPOSE OR THAT THE USE  \n" +
                "  20 OF THE LICENSED SOFTWARE, DATABASE OR DOCUMENTATION WILL NOT  \n" +
                "  21 INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR  \n" +
                "  22 OTHER RIGHTS.  \n" +
                "  23 The name of Princeton University or Princeton may not be used in  \n" +
                "  24 advertising or publicity pertaining to distribution of the software  \n" +
                "  25 and/or database.  Title to copyright in this software, database and  \n" +
                "  26 any associated documentation shall at all times remain with  \n" +
                "  27 Princeton University and LICENSEE agrees to preserve same.  \n" +
                "  28 \n" +
                "  29 Ground by oewntk@gmail.com     \n"
}