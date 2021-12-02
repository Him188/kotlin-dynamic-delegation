package example.kotlin.compiler.plugin.template.compiler.backend

import example.kotlin.compiler.plugin.template.compiler.config.PluginConfiguration
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.assertCast
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.util.transformDeclarationsFlat
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class PluginIrGenerationExtension(
    private val ext: PluginConfiguration
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        for (file in moduleFragment.files) {
            DynamicDelegationLoweringPass(pluginContext, ext).runOnFilePostfix(file)
            RemoveDelegateFieldInitializer(pluginContext, ext).runOnFilePostfix(file)
        }
    }
}

internal fun ClassLoweringPass.runOnFileInOrder(irFile: IrFile) {
    irFile.acceptVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitClass(declaration: IrClass) {
            lower(declaration) // lower delegation by ourselves.
        }
    })
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

object DynamicDelegationSymbols {
    val DEFAULT = FqName("me.him188.kotlin.dynamic.delgation.dynamicDelegation")
}

class RemoveDelegateFieldInitializer(
    private val context: IrPluginContext,
    private val ext: PluginConfiguration,
) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        val symbols = context.referenceFunctions(DynamicDelegationSymbols.DEFAULT)
        irClass.transformDeclarationsFlat {
            if (it is IrField && it.isDynamicDelegationField(symbols)) {
                it.initializer = null // TODO: 01/12/2021 optimize
                listOf(it)
            } else null
        }
    }
}

class DynamicDelegationLoweringPass(
    private val context: IrPluginContext,
    private val ext: PluginConfiguration,
) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        val dynamicDelegationSymbols = context.referenceFunctions(DynamicDelegationSymbols.DEFAULT)

        val delegateFields =
            irClass.declarations
                .filterIsInstance<IrField>()
                .filter { it.isDynamicDelegationField(dynamicDelegationSymbols) }
                .map {
                    DynamicDelegation(it, generateDynamicDelegationWrapper(it))
                }

        for ((_, function) in delegateFields) {
            irClass.declarations.add(function)
        }

        irClass.transform(DynamicExtensionClassTransformer(context, delegateFields), null)
    }

    private fun generateDynamicDelegationWrapper(field: IrField): IrSimpleFunction {
        fun extractActualCall(): IrExpression {
            // initializer
            return field.initializer!!.expression.assertCast<IrCall>().getValueArgument(0)!!
        }

        return IrFactoryImpl.buildFun {
            name = Name.identifier("${field.name}-wrapper")
            startOffset = field.startOffset
            endOffset = field.endOffset

            origin = DYNAMIC_DELEGATION_WRAPPER
            returnType = field.type

            visibility = DescriptorVisibilities.PRIVATE
        }.apply wrapper@{
            parent = field.parent
            dispatchReceiverParameter = field.parentAsClass.thisReceiver
            body = context.createIrBuilder(symbol).run {
                val invoke =
                    this@DynamicDelegationLoweringPass.context.symbols.functionN(0).getSimpleFunction("invoke")!!
                irExprBody(irCall(invoke, extractActualCall().assertCast<IrFunctionExpression>()))
            }
        }

    }
}

@Suppress("ClassName")
private object DYNAMIC_DELEGATION_WRAPPER : IrDeclarationOriginImpl("DYNAMIC_DELEGATION_WRAPPER", true)

internal fun IrPluginContext.createIrBuilder(
    symbol: IrSymbol,
    startOffset: Int = UNDEFINED_OFFSET,
    endOffset: Int = UNDEFINED_OFFSET,
) = DeclarationIrBuilder(this, symbol, startOffset, endOffset)


data class DynamicDelegation(
    val field: IrField,
    val wrapper: IrSimpleFunction,
)

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

fun IrBuilderWithScope.irCall(callee: IrSimpleFunctionSymbol, dispatchReceiver: IrExpression): IrCall =
    irCall(callee, callee.owner.returnType).apply {
        this.dispatchReceiver = dispatchReceiver
    }


class DynamicExtensionClassTransformer(
    private val context: IrPluginContext,
    private val delegations: List<DynamicDelegation>
) : IrElementTransformerVoidWithContext() {
    override fun visitExpression(expression: IrExpression): IrExpression {
        return super.visitExpression(expression)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
        if (declaration !is IrSimpleFunction) return super.visitDeclaration(declaration)

        if (declaration.origin == IrDeclarationOrigin.DELEGATED_MEMBER) {
            val delegation = declaration.findResponsibleDelegation(delegations)
                ?: return super.visitDeclaration(declaration)

            val delegateCall = (declaration.body?.statements?.single() as IrReturn).value.assertCast<IrCall>()

            declaration.body = context.createIrBuilder(declaration.symbol).run {
                val delegateInstance = irCall(
                    delegation.wrapper.symbol,
                    irGetObject(declaration.parentAsClass.symbol)
                )

                irBlockBody {
                    +irReturn(irCall(delegateCall.symbol, delegateInstance))
                }
            }
        }
        return super.visitDeclaration(declaration)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
//        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
//            override fun visitGetField(expression: IrGetField): IrExpression {
//                val (_, wrapper) = delegations.find { expression.symbol == it.field.symbol }
//                    ?: return super.visitGetField(expression)
//
//                return context.createIrBuilder(declaration.symbol).irCall(wrapper.symbol, expression.type)
//            }
//        })
        return super.visitSimpleFunction(declaration)
    }
}