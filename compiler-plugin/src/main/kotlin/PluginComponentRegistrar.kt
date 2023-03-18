package me.him188.kotlin.dynamic.delegation.compiler

import com.google.auto.service.AutoService
import me.him188.kotlin.dynamic.delegation.compiler.backend.PluginIrGenerationExtension
import me.him188.kotlin.dynamic.delegation.compiler.config.PluginConfigurationImpl
import me.him188.kotlin.dynamic.delegation.compiler.diagnostics.DynamicDelegationCallChecker
import me.him188.kotlin.dynamic.delegation.compiler.diagnostics.PersistentCallChecker
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
open class PluginComponentRegistrar @JvmOverloads constructor(
    private val overrideConfigurations: CompilerConfiguration? = null,
) : CompilerPluginRegistrar() {
    override val supportsK2: Boolean
        get() = false

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val actualConfiguration = overrideConfigurations ?: configuration

        val ext = actualConfiguration.createPluginConfig()

        StorageComponentContainerContributor.registerExtension(object : StorageComponentContainerContributor {
            override fun registerModuleComponents(
                container: StorageComponentContainer,
                platform: TargetPlatform,
                moduleDescriptor: ModuleDescriptor,
            ) {
                container.useInstance(
                    DynamicDelegationCallChecker()
                )
                container.useInstance(PersistentCallChecker())
            }
        })
        IrGenerationExtension.registerExtension(PluginIrGenerationExtension(ext))

    }
}

fun CompilerConfiguration.createPluginConfig(): PluginConfigurationImpl {
    val actualConfiguration = this

    val example =
        actualConfiguration[PluginCompilerConfigurationKeys.EXAMPLE] // parse example

    val ext = PluginConfigurationImpl()

    return ext
}
