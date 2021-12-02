package example.kotlin.compiler.plugin.template.compiler.diagnostics

import com.intellij.psi.PsiElement
import example.kotlin.compiler.plugin.template.compiler.backend.DynamicDelegationFqNames
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class DynamicDelegationCallChecker(
    private val isIr: Boolean
) : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val fqn = resolvedCall.resultingDescriptor.fqNameSafe
        if (fqn == DynamicDelegationFqNames.DEFAULT) {
            if (!isIr) {
                context.trace.report(Errors.DYNAMIC_DELEGATION_REQUIRES_IR.on(reportOn))
                return
            }

            val element = resolvedCall.call.callElement
            println(element)
        }
    }
}