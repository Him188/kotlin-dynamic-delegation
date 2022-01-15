@file:Suppress("UNUSED_PARAMETER")

package me.him188.kotlin.dynamic.delegation

import kotlin.reflect.KProperty1

/**
 * Creates a dynamic delegation.
 *
 * Example of usage:
 * ```
 * public interface PluginManager {
 *    public fun loadPlugin(plugin: Plugin)
 *
 *    public companion object INSTANCE : PluginManager by (dynamicDelegation { getInstanceFromOtherPlaces() })
 * }
 * ```
 */
public fun <R> dynamicDelegation(value: () -> R): R =
    throw NotImplementedError("Implemented as intrinsic")

/**
 * Creates a dynamic delegation.
 *
 * Type [T] must be the same as, or a supertype of, the class that uses `dynamicDelegation`.
 *
 * Example of usage:
 * ```
 * internal class CombinedMessage(
 *     val element: Message,
 *     val tail: Message
 * ) : MessageChain, List<SingleMessage> by dynamicDelegation(CombinedMessage::refined) {
 *     val refined: List<SingleMessage> by lazy {
 *         // lazily initialize the List<SingleMessage>
 *     }
 * }
 * ```
 */
public fun <T, R> dynamicDelegation(reference: KProperty1<T, R>): R =
    throw NotImplementedError("Implemented as intrinsic")