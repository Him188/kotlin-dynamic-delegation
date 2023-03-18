package me.him188.kotlin.dynamic.delegation.idea.highlighting

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.psi.PsiElement
import me.him188.kotlin.dynamic.delegation.compiler.backend.DDFqNames
import me.him188.kotlin.dynamic.delegation.idea.settings.DynamicDelegationHighlightingColors
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.codeinsight.utils.findExistingEditor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

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

            val fqn =
                function.bodyExpression?.resolveToCall(bodyResolveMode = BodyResolveMode.PARTIAL_NO_ADDITIONAL)?.candidateDescriptor?.fqNameOrNull()
                    ?: return
            if (!DDFqNames.isCallingPersistent(fqn)) return


            holder.add(
                HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION)
                    .range(target.textRange)
                    .textAttributes(editor.colorsScheme.getAttributes(DynamicDelegationHighlightingColors.PERSISTENT))
                    .create()
            )
        }
    }
}