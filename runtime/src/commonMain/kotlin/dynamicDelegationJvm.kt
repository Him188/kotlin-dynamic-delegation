@file:Suppress("UNUSED_PARAMETER")

package me.him188.kotlin.dynamic.delgation

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