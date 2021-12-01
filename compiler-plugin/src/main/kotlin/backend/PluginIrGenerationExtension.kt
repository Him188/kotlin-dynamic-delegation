package example.kotlin.compiler.plugin.template.compiler.backend

import example.kotlin.compiler.plugin.template.compiler.config.IPluginConfiguration
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class PluginIrGenerationExtension(
    private val ext: IPluginConfiguration
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {

    }
}