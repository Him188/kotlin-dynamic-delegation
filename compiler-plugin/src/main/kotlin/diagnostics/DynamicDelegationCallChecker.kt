package me.him188.kotlin.dynamic.delegation.compiler.diagnostics

import com.intellij.psi.PsiElement
import me.him188.kotlin.dynamic.delegation.compiler.backend.DynamicDelegationFqNames
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class DynamicDelegationCallChecker(
    private val isIr: (PsiElement) -> Boolean,
) : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val fqn = resolvedCall.resultingDescriptor.fqNameSafe
        if (fqn == DynamicDelegationFqNames.DEFAULT) {
            if (!isIr(reportOn)) {
                context.trace.report(Errors.DYNAMIC_DELEGATION_REQUIRES_IR.on(reportOn))
                return
            }

            val element = resolvedCall.call.callElement
            if (element.parents.filterNot { it is KtParenthesizedExpression }
                    .firstOrNull() !is KtDelegatedSuperTypeEntry) {
                context.trace.report(Errors.DYNAMIC_DELEGATION_NOT_ALLOWED.on(reportOn))
                return
            }

            checkPropertyReceiver(context, resolvedCall, reportOn)
        }
    }

    private fun checkPropertyReceiver(
        context: CallCheckerContext,
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement
    ) {
        val containingClassType =
            context.resolutionContext.expectedType

        val argument = resolvedCall.valueArgumentsByIndex?.firstOrNull() as? ExpressionValueArgument ?: return
        val argumentType =
            argument.valueArgument?.getArgumentExpression()?.getType(context.trace.bindingContext) ?: return

        if (argumentType.constructor.declarationDescriptor?.defaultType?.isSubtypeOf(context.moduleDescriptor.builtIns.kProperty1.defaultType) != true) return
        val propertyReferenceReceiver = argumentType.arguments.firstOrNull()?.type ?: return

        if (!propertyReferenceReceiver.isSubtypeOf(containingClassType)) {
            context.trace.report(
                Errors.INCOMPATIBLE_PROPERTY_RECEIVER.on(
                    argument.valueArgument?.getArgumentExpression()?.children?.firstOrNull()
                        ?: reportOn, // report on receiver - KtNameReferenceExpression
                    resolvedCall.call.callElement.containingClass()?.nameAsSafeName ?: return
                )
            )
            return
        }
    }
}