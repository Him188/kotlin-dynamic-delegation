package me.him188.kotlin.dynamic.delegation.compiler

import org.jetbrains.kotlin.config.CompilerConfigurationKey

object PluginCompilerConfigurationKeys {
    @JvmStatic
    val EXAMPLE =
        CompilerConfigurationKeyWithName<String>("example")
}

// don't data
class CompilerConfigurationKeyWithName<T>(val name: String) : CompilerConfigurationKey<T>(name)