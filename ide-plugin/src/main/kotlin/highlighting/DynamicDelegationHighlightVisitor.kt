package me.him188.kotlin.dynamic.delegation.idea.highlighting

import com.intellij.codeInsight.daemon.impl.HighlightVisitor
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtVisitorVoid

class DynamicDelegationHighlightVisitor : HighlightVisitor {

    override fun suitableForFile(file: PsiFile): Boolean = file is KtFile

    override fun visit(element: PsiElement) {
        afterAnalysisVisitors?.forEach { element.accept(it) }
    }

    @JvmField
    var afterAnalysisVisitors: Array<KtVisitorVoid>? = null

    override fun analyze(
        file: PsiFile,
        updateWholeFile: Boolean,
        holder: HighlightInfoHolder,
        action: Runnable
    ): Boolean {
        try {
            afterAnalysisVisitors = arrayOf(
                DynamicDelegationVisitor(holder),
                PersistentVisitor(holder),
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

    override fun clone(): HighlightVisitor = DynamicDelegationHighlightVisitor()

    companion object {
        val LOG = Logger.getInstance(DynamicDelegationHighlightVisitor::class.java)
    }
}