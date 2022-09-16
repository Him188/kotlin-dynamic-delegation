package me.him188.kotlin.dynamic.delegation.compiler.backend

import me.him188.kotlin.dynamic.delegation.compiler.config.PluginConfiguration
import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.parentsWithSelf
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class PluginIrGenerationExtension(
    private val ext: PluginConfiguration
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val context = PluginGenerationContext(pluginContext, ext)
        for (file in moduleFragment.files) {
            DynamicDelegationLoweringPass(context).runOnFilePostfix(file)
            RemoveDelegateFieldInitializerPass(pluginContext, ext).runOnFilePostfix(file)
            PersistentLoweringPass(context).runOnFilePostfix(file)
        }
    }
}

data class PluginGenerationContext(
    val irContext: IrPluginContext,
    val ext: PluginConfiguration,
    val wrapperExpressionMapper: WrapperExpressionMapper = WrapperExpressionMapper(irContext, ext),
)

class RemoveDelegateFieldInitializerPass(
    private val context: IrPluginContext,
    private val ext: PluginConfiguration,
) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        val symbols = context.referenceFunctions(DDFqNames.DYNAMIC_DELEGATION)
        irClass.transformDeclarationsFlat {
            if (it is IrField && it.isDynamicDelegationField(symbols)) {
                it.initializer = null // not removing the field for compatibility
                listOf(it)
            } else null
        }
    }
}

class PersistentLoweringPass(
    private val pluginContext: PluginGenerationContext,
) : BodyLoweringPass {
    private val irContext get() = pluginContext.irContext

    private val persistentSymbols = pluginContext.irContext.referenceFunctions(DDFqNames.PERSISTENT)
    private val lazyClassSymbol = pluginContext.irContext.referenceClass(DDFqNames.LAZY_CLASS)!!
    private val lazyGetValueSymbol = lazyClassSymbol.getPropertyGetter("value")!!
    private val lazyFunSymbol: IrSimpleFunctionSymbol =
        pluginContext.irContext.referenceFunctions(DDFqNames.LAZY_FUN).single { function ->
            val valueParameters = function.owner.valueParameters
            valueParameters.size == 1 && valueParameters.single().type.getClass() == pluginContext.irContext.irBuiltIns.functionN(
                0
            )
        }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol !in persistentSymbols) {
                    return expression
                }
                return processCall(expression, container) ?: expression
            }
        })
    }

    // statement is calling `persistent`
    private fun processCall(statement: IrCall, container: IrDeclaration): IrExpression? {
        if (statement.typeArgumentsCount == 0) return null
        if (statement.valueArgumentsCount == 0) return null
        if (container !is IrSimpleFunction) return null

        val declarationContainer = container.parentsWithSelf.firstIsInstance<IrDeclarationContainer>()
        if (declarationContainer !is IrSymbolOwner) return null

        val propertyName = nextPropertyName(container, declarationContainer)

        val originTypeArgument = statement.getTypeArgument(0)!!

        val field = pluginContext.irContext.irFactory.buildField {
            isStatic = declarationContainer is IrFile
            name = propertyName
            type = lazyClassSymbol.typeWith(originTypeArgument)
        }.also { field ->
            field.parent = declarationContainer
            declarationContainer.addChild(field)
        }

        field.initializer = pluginContext.irContext.createIrBuilder(field.symbol).run {
            //  = lazy { statement.call() }
            irExprBody(irCall(lazyFunSymbol).apply {
                val originDispatchReceiverType = container.dispatchReceiverParameter?.type
                putValueArgument(0, statement.getValueArgument(0)!!.apply {
                    transformDispatchReceivers(declarationContainer, field, originDispatchReceiverType)
                })
            })
        }

        // replace `persistent {  }` with `this.fn$pers1` (get field)
        return pluginContext.irContext.createIrBuilder(container.symbol).run {
            val lazy = irGetField(
                container.dispatchReceiverParameter?.let(::irGet),
                field
            ) // IrFile doesn't need dispatch receiver
            irCall(lazyGetValueSymbol, dispatchReceiver = lazy)
        }
    }

    private fun IrExpression.transformDispatchReceivers(
        declarationContainer: IrDeclarationContainer,
        field: IrField,
        originDispatchReceiverType: IrType?
    ) {
        if (declarationContainer !is IrClass) return
        val builder = pluginContext.irContext.createIrBuilder(field.symbol)
        val newDispatchReceiver = declarationContainer.thisReceiver ?: return
        transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                if (expression.type == originDispatchReceiverType) {
                    return builder.irGet(newDispatchReceiver)
                }

                return super.visitGetValue(expression)
            }
        })
    }

    private fun nextPropertyName(container: IrDeclaration, declarationContainer: IrDeclarationContainer): Name {
        var shift = 1
        while (true) {
            val name = Name.identifier(container.getName().asString() + "\$pers$shift")
            if (declarationContainer.declarations.any { it.getNameOrNull() == name }) {
                shift++
            } else {
                return name
            }
        }
    }

    private fun IrDeclaration.getName(): Name {
        return getNameOrNull() ?: error("No name: $this")
    }

    private fun IrDeclaration.getNameOrNull(): Name? {
        return if (this is IrDeclarationWithName) name else null
    }
}

class DynamicDelegationLoweringPass(
    private val pluginContext: PluginGenerationContext,
) : ClassLoweringPass {
    private val dynamicDelegationSymbols =
        pluginContext.irContext.referenceFunctions(DDFqNames.DYNAMIC_DELEGATION)

    override fun lower(irClass: IrClass) {
        val delegateFields =
            irClass.fields
                .filter { it.isDynamicDelegationField(dynamicDelegationSymbols) }
                .map {
                    DynamicDelegation(it, generateDynamicDelegationWrapper(it))
                }
                .toList()

        delegateFields.asSequence().flatMap { it.wrapper.declarations() }.forEach { irClass.declarations.add(it) }

        irClass.transform(DynamicExtensionClassTransformer(pluginContext, delegateFields.toList()), null)
    }

    /**
     * The wrapper returns the delegated instance
     */
    private fun generateDynamicDelegationWrapper(field: IrField): DynamicDelegationWrapper {
        fun extractActualCall(): IrExpression {
            // initializer
            return (field.initializer?.expression as IrCall?)?.getValueArgument(0)
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
            body = pluginContext.irContext.createIrBuilder(symbol).irExprBody(expr)

            DynamicDelegationWrapper(this, helperField)
        }
    }
}


/**
 * Transform [IrDeclarationOrigin.DELEGATED_MEMBER] from `irGetField` to `irCall(wrapper)`
 */
class DynamicExtensionClassTransformer(
    private val context: PluginGenerationContext,
    private val delegations: List<DynamicDelegation>
) : IrElementTransformerVoidWithContext() {
    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
        if (declaration !is IrSimpleFunction) return super.visitDeclaration(declaration)

        if (declaration.origin == IrDeclarationOrigin.DELEGATED_MEMBER) {
            val delegation = declaration.findResponsibleDelegation(delegations)
                ?: return super.visitDeclaration(declaration)

            val delegateCall = when (val expr = declaration.body?.statements?.single()) {
                is IrReturn -> expr.value as IrCall
                is IrCall -> expr
                null -> return super.visitDeclaration(declaration)
                else -> error("Unsupported expr: ${expr::class.qualifiedName} $expr")
            }

            declaration.body = context.irContext.createIrBuilder(declaration.symbol).run {
                val delegateInstance = irCall(
                    delegation.wrapper.function.symbol,
                    dispatchReceiver = declaration.dispatchReceiverParameter?.let { irGet(it) }
                )

                irExprBody(irCall(delegateCall.symbol, dispatchReceiver = delegateInstance).apply {
                    copyExtensionReceiverFrom(delegateCall)
                    copyArgumentsFrom(delegateCall)
                })
            }
        }
        return super.visitDeclaration(declaration)
    }
}

internal fun IrCall.copyArgumentsFrom(delegateCall: IrCall) {
    repeat(delegateCall.valueArgumentsCount) {
        putValueArgument(it, delegateCall.getValueArgument(it))
    }
    repeat(delegateCall.typeArgumentsCount) {
        putTypeArgument(it, delegateCall.getTypeArgument(it))
    }
}

private fun IrCall.copyExtensionReceiverFrom(delegateCall: IrCall) {
    this.extensionReceiver = delegateCall.extensionReceiver
}