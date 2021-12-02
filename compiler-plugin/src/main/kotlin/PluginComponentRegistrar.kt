package example.kotlin.compiler.plugin.template.compiler

import com.google.auto.service.AutoService
import com.intellij.mock.MockProject
import example.kotlin.compiler.plugin.template.compiler.backend.PluginIrGenerationExtension
import example.kotlin.compiler.plugin.template.compiler.config.PluginConfigurationImpl
import example.kotlin.compiler.plugin.template.compiler.diagnostics.DynamicDelegationCallChecker
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform

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

        StorageComponentContainerContributor.registerExtension(project, object : StorageComponentContainerContributor {
            override fun registerModuleComponents(
                container: StorageComponentContainer,
                platform: TargetPlatform,
                moduleDescriptor: ModuleDescriptor,
            ) {
                container.useInstance(
                    DynamicDelegationCallChecker(actualConfiguration[JVMConfigurationKeys.IR, false])
                )
            }
        })
        IrGenerationExtension.registerExtension(project, PluginIrGenerationExtension(ext))
    }
}

fun CompilerConfiguration.createPluginConfig(): PluginConfigurationImpl {
    val actualConfiguration = this

    val example =
        actualConfiguration[PluginCompilerConfigurationKeys.EXAMPLE] // parse example

    val ext = PluginConfigurationImpl()

    return ext
}
