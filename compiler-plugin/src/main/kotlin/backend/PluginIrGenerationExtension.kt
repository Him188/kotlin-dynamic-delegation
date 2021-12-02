package example.kotlin.compiler.plugin.template.compiler.backend

import example.kotlin.compiler.plugin.template.compiler.config.PluginConfiguration
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.assertCast
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.*
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
            DynamicDelegationLoweringPass(PluginGenerationContext(pluginContext, ext)).runOnFilePostfix(file)
            RemoveDelegateFieldInitializer(pluginContext, ext).runOnFilePostfix(file)
        }
    }
}

data class PluginGenerationContext(
    val context: IrPluginContext,
    val ext: PluginConfiguration,
    val wrapperExpressionMapper: WrapperExpressionMapper = WrapperExpressionMapper(context, ext),
) : IrPluginContext by context {

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
    private val pluginContext: PluginGenerationContext,
) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        val dynamicDelegationSymbols = pluginContext.context.referenceFunctions(DynamicDelegationSymbols.DEFAULT)

        val delegateFields =
            irClass.declarations
                .filterIsInstance<IrField>()
                .filter { it.isDynamicDelegationField(dynamicDelegationSymbols) }
                .map {
                    DynamicDelegation(it, generateDynamicDelegationWrapper(it))
                }

        for ((_, function) in delegateFields) {
            for (declaration in function.declarations()) {
                irClass.declarations.add(declaration)
            }
        }

        irClass.transform(DynamicExtensionClassTransformer(pluginContext, delegateFields), null)
    }

    /**
     * The wrapper returns the delegated instance
     */
    private fun generateDynamicDelegationWrapper(field: IrField): DynamicDelegationWrapper {
        fun extractActualCall(): IrExpression {
            // initializer
            return field.initializer?.expression?.assertCast<IrCall>()?.getValueArgument(0)
                ?: error("Could not find an initializer for DELEGATE field '${field.render()}'.")
        }

        return IrFactoryImpl.buildFun {
            name = Name.identifier("${field.name}-wrapper")
            startOffset = field.startOffset
            endOffset = field.endOffset

            origin = DYNAMIC_DELEGATION_WRAPPER
            returnType = field.type

            visibility = DescriptorVisibilities.PRIVATE
        }.run wrapper@{
            parent = field.parent
            dispatchReceiverParameter = field.parentAsClass.thisReceiver?.copyTo(this)
            val (expr, helperField) = pluginContext.wrapperExpressionMapper.map(symbol, extractActualCall())
            body = pluginContext.createIrBuilder(symbol).irExprBody(expr)

            DynamicDelegationWrapper(this, helperField)
        }
    }
}

class DynamicDelegationWrapper(
    val function: IrSimpleFunction,
    val helperField: IrField?,
) {
    fun declarations(): Sequence<IrDeclaration> = sequenceOf(function, helperField).filterNotNull()
}

/**
 * Maps the argument of 'dynamicDelegation' into a call for the wrapper function body.
 */
class WrapperExpressionMapper(
    private val context: IrPluginContext,
    private val ext: PluginConfiguration,
) {
    val pluginContext get() = context

    val invoke =
        context.symbols.functionN(0).getSimpleFunction("invoke")!!

    data class MapResult(
        val expression: IrExpression,
        val helperField: IrField?,
    )

    fun map(ownerSymbol: IrSimpleFunctionSymbol, argumentExpression: IrExpression): MapResult {
        fun IrBuilderWithScope.irGetProperty(
            property: IrPropertyReference
        ): IrExpression {
            property.getter?.let { getter -> return irCall(getter) }
            property.field?.let { field ->
                return irGetField(property.extensionReceiver ?: property.dispatchReceiver, field.owner)
            }
            error("Could not find a valid getter or backing field for property ${property.render()}")
        }

        return context.createIrBuilder(ownerSymbol).run {
            // optimizations
            val optimized = when (argumentExpression) {
                is IrFunctionExpression -> irCall(invoke, argumentExpression) // { getInstanceFromOtherPlaces() }
                is IrFunctionReference -> irCall(argumentExpression.symbol) // ::getInstanceFromOtherPlaces
                is IrPropertyReference -> irGetProperty(argumentExpression) // ::property  // with getter/backingField
                is IrBlock -> irCall(invoke, argumentExpression) // TestObject::getInstanceFromOtherPlaces
                else -> null
            }
            if (optimized != null) MapResult(optimized, null)
            else {
                // Calling some other expressions which can't be optimized, for example, a property whose type is () -> T
                // We should introduce a field


                val field = context.irFactory.buildField {
                    startOffset = ownerSymbol.owner.startOffset
                    endOffset = ownerSymbol.owner.endOffset
                    name = Name.identifier(ownerSymbol.owner.name.asString() + "\$wrapper")
                    type = argumentExpression.type
                    origin = DYNAMIC_DELEGATION_WRAPPER
                    visibility = DescriptorVisibilities.PRIVATE
                }.apply {
                    parent = ownerSymbol.owner.parentAsClass
                    initializer = pluginContext.createIrBuilder(symbol).run {
                        irExprBody(argumentExpression)
                    }
                }

                // TODO: 02/12/2021 how about local functions? it should be a variable instead.
                MapResult(
                    irCall(invoke, irGetField(irGet(ownerSymbol.owner.dispatchReceiverParameter!!), field)),
                    field
                )
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
    val wrapper: DynamicDelegationWrapper,
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

fun IrBuilderWithScope.irCall(callee: IrSimpleFunctionSymbol, dispatchReceiver: IrExpression?): IrCall =
    irCall(callee, callee.owner.returnType).apply {
        this.dispatchReceiver = dispatchReceiver
    }


class DynamicExtensionClassTransformer(
    private val context: PluginGenerationContext,
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
                    delegation.wrapper.function.symbol,
                    dispatchReceiver = declaration.dispatchReceiverParameter?.let { irGet(it) }
                )

                irExprBody(irCall(delegateCall.symbol, dispatchReceiver = delegateInstance))
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