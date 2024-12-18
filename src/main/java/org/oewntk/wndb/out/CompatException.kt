/*
 * Copyright (c) 2024. Bernard Bou.
 */
package org.oewntk.wndb.out

/**
 * Exception raised on compatibility issues
 */
class CompatException : Exception {

    @Suppress("unused")
    constructor(message: String) : super(message)

    @Suppress("unused")
    constructor(message: String, cause: Throwable?) : super(message, cause)

    constructor(cause: Throwable) : super(cause)
}
