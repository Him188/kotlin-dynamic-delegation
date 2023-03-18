package me.him188.kotlin.dynamic.delegation.compiler.diagnostics

import com.intellij.psi.PsiElement
import me.him188.kotlin.dynamic.delegation.compiler.backend.DDFqNames
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class DynamicDelegationCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val fqn = resolvedCall.resultingDescriptor.fqNameSafe
        if (fqn == DDFqNames.DYNAMIC_DELEGATION.asSingleFqName()) {
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
        val containingClass = resolvedCall.call.callElement.containingClass()
            ?.getDescriptor(context.trace.bindingContext) as? ClassDescriptor ?: return

        val containingClassType = containingClass.defaultType

        val argument = resolvedCall.valueArgumentsByIndex?.firstOrNull() as? ExpressionValueArgument ?: return
        val argumentType =
            argument.valueArgument?.getArgumentExpression()?.getType(context.trace.bindingContext) ?: return

        if (argumentType.constructor.declarationDescriptor?.defaultType?.isSubtypeOf(context.moduleDescriptor.builtIns.kProperty1.defaultType) != true) return
        val referenceReceiverType = argumentType.arguments.firstOrNull()?.type ?: return

        if (containingClassType != referenceReceiverType && !containingClassType.isSubtypeOf(referenceReceiverType)) {
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

internal fun KtExpression.getDescriptor(context: BindingContext): DeclarationDescriptor? =
    context[BindingContext.DECLARATION_TO_DESCRIPTOR, this]
