package me.him188.kotlin.dynamic.delegation.compiler.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.name.Name

object Errors : DefaultErrorMessages.Extension {
    @JvmField
    var COMPILER_PLUGIN_NOT_ENABLED = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)

    @JvmField
    var DYNAMIC_DELEGATION_NOT_ALLOWED = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)

    @JvmField
    var DYNAMIC_DELEGATION_REQUIRES_IR = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)

    @JvmField
    var INCOMPATIBLE_PROPERTY_RECEIVER = DiagnosticFactory1.create<PsiElement, Name>(Severity.ERROR)

    @JvmField
    var UNSUPPORTED_BLOCK_BODY = DiagnosticFactory0.create<PsiElement>(Severity.ERROR)


    private val map = DiagnosticFactoryToRendererMap("JvmBlockingBridge").apply {
        put(
            COMPILER_PLUGIN_NOT_ENABLED,
            "Dynamic delegation compiler plugin is not applied to the module, so this function would not be processed and will cause NotImplementedError in runtime. " +
                    "Make sure that you've setup your buildscript correctly and re-import project."
        )

        put(
            DYNAMIC_DELEGATION_NOT_ALLOWED,
            "Dynamic delegation is not allowed here. It should only be used in class delegations."
        )

        put(
            DYNAMIC_DELEGATION_REQUIRES_IR,
            "Dynamic delegation is only supported in IR backend."
        )

        put(
            INCOMPATIBLE_PROPERTY_RECEIVER,
            "Property receiver is not compatible with type ''{0}''",
            Renderers.TO_STRING
        )

        put(
            UNSUPPORTED_BLOCK_BODY,
            "'persistent' is not allowed in block body.",
        )
    }

    init {
        org.jetbrains.kotlin.diagnostics.Errors.Initializer.initializeFactoryNamesAndDefaultErrorMessages(
            this::class.java,
            this
        )
    }

    override fun getMap(): DiagnosticFactoryToRendererMap = map
}