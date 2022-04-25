package me.him188.kotlin.dynamic.delegation.idea.highlighting

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.psi.PsiElement
import me.him188.kotlin.dynamic.delegation.idea.settings.DynamicDelegationHighlightingColors
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.psi.*

class PersistentVisitor(
    private val holder: HighlightInfoHolder,
) : KtVisitorVoid() {
    override fun visitElement(element: PsiElement) {
        element.acceptChildren(this)
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        if (function.bodyExpression !is KtBlockExpression) {
            val target =
                (function.bodyExpression as? KtCallExpression)?.children?.find { it is KtNameReferenceExpression }
                    ?: return
            val editor = target.findExistingEditor() ?: return

            holder.add(
                HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION)
                    .range(target.textRange)
                    .textAttributes(editor.colorsScheme.getAttributes(DynamicDelegationHighlightingColors.PERSISTENT))
                    .create()
            )
        }
    }
}