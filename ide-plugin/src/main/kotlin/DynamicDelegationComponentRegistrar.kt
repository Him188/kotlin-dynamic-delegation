package me.him188.kotlin.dynamic.delegation.idea

import me.him188.kotlin.dynamic.delegation.compiler.diagnostics.DynamicDelegationCallChecker
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.platform.TargetPlatform

class DynamicDelegationComponentRegistrar : StorageComponentContainerContributor {
    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor
    ) {
        container.useInstance(DynamicDelegationCallChecker { it.module?.isIr != false })
    }
}
