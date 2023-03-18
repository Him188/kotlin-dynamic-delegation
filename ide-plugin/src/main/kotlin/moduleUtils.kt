package me.him188.kotlin.dynamic.delegation.idea

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import me.him188.kotlin.dynamic.delegation.compiler.DynamicDelegationCommandLineProcessor
import me.him188.kotlin.dynamic.delegation.compiler.config.PluginConfiguration
import me.him188.kotlin.dynamic.delegation.compiler.createPluginConfig
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KotlinFacetSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.base.projectStructure.unwrapModuleSourceInfo
import org.jetbrains.kotlin.idea.base.util.module
import org.jetbrains.kotlin.idea.caches.project.toDescriptor
import org.jetbrains.kotlin.idea.facet.KotlinFacet

class DynamicDelegationModuleCacheService {
    var compilerEnabled: Boolean = true
    var isIr: Boolean = true
    var config: PluginConfiguration = PluginConfiguration.Default

    var initialized = false
}

val Module.pluginConfiguration
    get() = useCacheOrInit { it.config } ?: PluginConfiguration.Default

val PsiElement.bridgeConfiguration
    get() = this.module?.pluginConfiguration ?: PluginConfiguration.Default

val Module.isIr
    get() = useCacheOrInit { it.isIr } ?: true

val PsiElement.isIr
    get() = module?.isIr == true

val Module.isDynamicDelegationEnabled
    get() = useCacheOrInit { it.compilerEnabled } ?: true

val PsiElement.isDynamicDelegationEnabled
    get() = module?.isDynamicDelegationEnabled ?: true


inline fun <R> Module.useCacheOrInit(
    useCache: (cache: DynamicDelegationModuleCacheService) -> R,
): R? {
    val module = this
    val moduleDescriptor = module.toDescriptor() ?: return null
    val cache = module.getService(DynamicDelegationModuleCacheService::class.java)
    if (cache.initialized) {
        return useCache(cache)
    }

    cache.isIr = moduleDescriptor.isIr()
    cache.config = moduleDescriptor.createBridgeConfig() ?: PluginConfiguration.Default
    cache.compilerEnabled = moduleDescriptor.isDynamicDelegationEnabled()
    cache.initialized = true

    return useCache(cache)
}


fun ModuleDescriptor.isIr(): Boolean {
    val compilerArguments = kotlinFacetSettings()?.compilerArguments ?: return true
    when (compilerArguments) {
        is K2JVMCompilerArguments -> {
            if (compilerArguments.useIR) return true
            if (compilerArguments.useOldBackend) return false
        }

        else -> {
            return true
        }
    }
    return true
}

//
fun ModuleDescriptor.createBridgeConfig(): PluginConfiguration? {
    return kotlinFacetSettings()?.compilerArguments?.createCompilerConfiguration()?.createPluginConfig()
}

private fun CommonCompilerArguments.createCompilerConfiguration(): CompilerConfiguration? {
    val pluginOptions = pluginOptions ?: return null

    fun findOption(option: CliOption): String? {
        return pluginOptions.find { it.startsWith("plugin:${DynamicDelegationCommandLineProcessor.COMPILER_PLUGIN_ID}:${option.optionName}=") }
            ?.substringAfter('=', "")
    }

    val processor = DynamicDelegationCommandLineProcessor()
    val configuration = CompilerConfiguration()

    for (pluginOption in processor.pluginOptions) {
        val find = findOption(pluginOption)
        if (find != null) {
            processor.processOption(pluginOption as AbstractCliOption, find, configuration)
        }
    }
    return configuration
}

fun ModuleDescriptor.kotlinFacetSettings(): KotlinFacetSettings? {
    val module =
        getCapability(ModuleInfo.Capability)?.unwrapModuleSourceInfo()?.module ?: return null
    val facet = KotlinFacet.get(module) ?: return null
    return facet.configuration.settings
}


fun ModuleDescriptor.isDynamicDelegationEnabled(): Boolean {
    val pluginJpsJarName = "kotlin-dynamic-delegation"
    val module =
        getCapability(ModuleInfo.Capability)?.unwrapModuleSourceInfo()?.module
            ?: return false
    val facet = KotlinFacet.get(module) ?: return false
    val pluginClasspath =
        facet.configuration.settings.compilerArguments?.pluginClasspaths ?: return false

    if (pluginClasspath.none { path -> path.contains(pluginJpsJarName) }) return false
    return true
}