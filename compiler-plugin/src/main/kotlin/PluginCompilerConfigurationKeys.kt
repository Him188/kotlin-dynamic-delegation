package example.kotlin.compiler.plugin.template.compiler

import org.jetbrains.kotlin.config.CompilerConfigurationKey

object PluginCompilerConfigurationKeys {
    @JvmStatic
    val EXAMPLE =
        CompilerConfigurationKeyWithName<String>("example")
}

// don't data
class CompilerConfigurationKeyWithName<T>(val name: String) : CompilerConfigurationKey<T>(name)