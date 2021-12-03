package me.him188.kotlin.dynamic.delegation.compiler

import com.google.auto.service.AutoService
import me.him188.kotlin.dynamic.delegation.compiler.PluginCompilerConfigurationKeys.EXAMPLE
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration

@AutoService(CommandLineProcessor::class)
class DynamicDelegationCommandLineProcessor : CommandLineProcessor {
    companion object {
        const val COMPILER_PLUGIN_ID: String = "kotlin-dynamic-delegation"

        val OPTION_EXAMPLE: CliOption = CliOption(
            EXAMPLE.name,
            "<example/example>",
            "Example key",
            required = false
        )
    }

    override val pluginId: String = COMPILER_PLUGIN_ID
    override val pluginOptions: Collection<CliOption> = listOf()

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
//        when (option) {
//            OPTION_EXAMPLE -> configuration.put(EXAMPLE, value)
//            else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
//        }
    }
}