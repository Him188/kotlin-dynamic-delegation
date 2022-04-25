package me.him188.kotlin.dynamic.delegation.idea.highlighting

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import me.him188.kotlin.dynamic.delegation.compiler.backend.DDFqNames
import me.him188.kotlin.dynamic.delegation.idea.settings.DynamicDelegationHighlightingColors
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class DynamicDelegationVisitor(
    private val holder: HighlightInfoHolder,
) : KtVisitorVoid() {
    //        override fun visitElement(element: PsiElement) {
//            super.visitElement(element)
//            element.acceptChildren(this)
//        }
    private fun highlightEntry(
        entry: KtDelegatedSuperTypeEntry,
        holder: HighlightInfoHolder,
    ) {
        val editor = entry.findExistingEditor() ?: return

        val fqn =
            entry.delegateExpression?.resolveToCall(bodyResolveMode = BodyResolveMode.PARTIAL_NO_ADDITIONAL)?.candidateDescriptor?.fqNameOrNull()
                ?: return
        if (!DDFqNames.isDynamicDelegation(fqn)) return

        val node = entry.byKeywordNode

        holder.add(
            HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION)
                .range(node.textRange)
                .descriptionAndTooltip("Dynamic delegation")
                .textAttributes(editor.colorsScheme.getAttributes(DynamicDelegationHighlightingColors.DYNAMIC_DELEGATION))
                .create()
        )
    }


    override fun visitDelegatedSuperTypeEntry(specifier: KtDelegatedSuperTypeEntry) {
        super.visitDelegatedSuperTypeEntry(specifier)
        highlightEntry(specifier, holder)
    }
}