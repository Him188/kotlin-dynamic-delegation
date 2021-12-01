package example.kotlin.compiler.plugin.template

import com.google.auto.service.AutoService
import com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration

@AutoService(ComponentRegistrar::class)
open class PluginComponentRegistrar @JvmOverloads constructor(
    private val overrideConfigurations: CompilerConfiguration? = null,
) : ComponentRegistrar {

    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration,
    ) {
        val actualConfiguration = overrideConfigurations ?: configuration

        val ext = actualConfiguration.createPluginConfig()

        TODO()
    }
}

fun CompilerConfiguration.createPluginConfig(): PluginConfigurationImpl {
    val actualConfiguration = this

    val example =
        actualConfiguration[PluginCompilerConfigurationKeys.EXAMPLE] // parse example

    val ext = PluginConfigurationImpl()

    return ext
}
