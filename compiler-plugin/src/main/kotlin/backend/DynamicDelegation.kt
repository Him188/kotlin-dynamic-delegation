package example.kotlin.compiler.plugin.template.compiler.backend

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName


data class DynamicDelegation(
    val field: IrField,
    val wrapper: DynamicDelegationWrapper,
)

object DynamicDelegationSymbols {
    val DEFAULT = FqName("me.him188.kotlin.dynamic.delgation.dynamicDelegation")
}

fun IrField.isDynamicDelegationField(dynamicDelegationSymbols: Collection<IrSymbol>): Boolean {
    if (origin != IrDeclarationOrigin.DELEGATE) return false
    val initializer = initializer ?: return false
    var result = false
    initializer.acceptChildrenVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            if (element is IrCall) {
                if (element.symbol in dynamicDelegationSymbols) {
                    result = true
                }
            }
        }
    })
    return result
}

fun IrSimpleFunction.findResponsibleDelegation(delegations: List<DynamicDelegation>): DynamicDelegation? {
    var result: DynamicDelegation? = null
    acceptChildrenVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitGetField(expression: IrGetField) {
            val newResult =
                delegations.find { expression.symbol == it.field.symbol } ?: return super.visitGetField(expression)
            if (result == null) {
                result = newResult
            } else {
                if (result != newResult) {
                    error("Found multiple matching delegations: \n$result\n$newResult")
                }
            }

            return super.visitGetField(expression)
        }
    })
    return result
}
