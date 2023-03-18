package me.him188.kotlin.dynamic.delegation.compiler.backend

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name


data class DynamicDelegation(
    val field: IrField,
    val wrapper: DynamicDelegationWrapper,
)

object DDFqNames {
    val DYNAMIC_DELEGATION = CallableId(
        packageName = FqName("me.him188.kotlin.dynamic.delegation"),
        callableName = Name.identifier("dynamicDelegation"),
    )
    val PERSISTENT = CallableId(
        packageName = FqName("me.him188.kotlin.dynamic.delegation"),
        callableName = Name.identifier("persistent"),
    )
    val LAZY_CLASS = ClassId.topLevel(FqName("kotlin.Lazy"))
    val LAZY_FUN = CallableId(
        packageName = FqName("kotlin"),
        callableName = Name.identifier("lazy")
    )

    fun isDynamicDelegation(name: FqName): Boolean {
        return name == DYNAMIC_DELEGATION.asSingleFqName()
    }

    fun isCallingPersistent(name: FqName): Boolean {
        return name == PERSISTENT.asSingleFqName()
    }
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
