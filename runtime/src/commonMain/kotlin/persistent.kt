@file:Suppress("UNUSED_PARAMETER")

package me.him188.kotlin.dynamic.delegation

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Declares a *persistent* value that will be lazily initialized exactly once using [block] and used afterwards.
 *
 * [block] will not be invoked until first invocation of this method.
 *
 * Examples of usages:
 * ```
 * override fun toString(): String = persistent { this.delegate.joinToString("") { it.toString() } }
 * ```
 * Is equivalent to
 * ```
 * private val toString$t1: Lazy<String> = lazy { this.delegate.joinToString("") { it.toString() } }
 * override fun toString(): String = toString$t1.value
 * ```
 */
public inline fun <R> persistent(block: () -> R): R {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) } // to enable relevant diagnostics
    throw NotImplementedError("Implemented as intrinsic")
}