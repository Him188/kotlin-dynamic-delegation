package me.him188.kotlin.dynamic.delegation.idea

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.HighlightVisitor
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import me.him188.kotlin.dynamic.delegation.compiler.backend.DynamicDelegationFqNames
import me.him188.kotlin.dynamic.delegation.idea.settings.DynamicDelegationHighlightingColors
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ByKeywordHighlightVisitor : HighlightVisitor {
    fun highlightEntry(
        entry: KtDelegatedSuperTypeEntry,
        holder: HighlightInfoHolder,
    ) {
        val editor = entry.findExistingEditor() ?: return

        val fqn =
            entry.delegateExpression?.resolveToCall(bodyResolveMode = BodyResolveMode.PARTIAL_NO_ADDITIONAL)?.candidateDescriptor?.fqNameOrNull()
                ?: return
        if (!DynamicDelegationFqNames.isDynamicDelegation(fqn)) return

        val node = entry.byKeywordNode

        holder.add(
            HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION)
                .range(node.textRange)
                .descriptionAndTooltip("Dynamic delegation")
                .textAttributes(editor.colorsScheme.getAttributes(DynamicDelegationHighlightingColors.BY_KEYWORD))
                .create()
        )
    }

    override fun suitableForFile(file: PsiFile): Boolean = file is KtFile

    inner class DynamicDelegationVisitor(
        private val holder: HighlightInfoHolder,
    ) : KtVisitorVoid() {
//        override fun visitElement(element: PsiElement) {
//            super.visitElement(element)
//            element.acceptChildren(this)
//        }


        override fun visitDelegatedSuperTypeEntry(specifier: KtDelegatedSuperTypeEntry) {
            super.visitDelegatedSuperTypeEntry(specifier)
            highlightEntry(specifier, holder)
        }
    }

    override fun visit(element: PsiElement) {
        afterAnalysisVisitors?.forEach { element.accept(it) }
//        element.acceptChildren(DynamicDelegationVisitor())
    }

    @JvmField
    var afterAnalysisVisitors: Array<DynamicDelegationVisitor>? = null

    override fun analyze(
        file: PsiFile,
        updateWholeFile: Boolean,
        holder: HighlightInfoHolder,
        action: Runnable
    ): Boolean {
        try {
            afterAnalysisVisitors = arrayOf(
                DynamicDelegationVisitor(holder)
            )
            action.run()
        } catch (e: Throwable) {
            if (e is ControlFlowException) throw e

            LOG.warn(e)
        } finally {
            afterAnalysisVisitors = null
        }

        return true
    }

    override fun clone(): HighlightVisitor = ByKeywordHighlightVisitor()

    companion object {
        val LOG = Logger.getInstance(ByKeywordHighlightVisitor::class.java)
    }
}