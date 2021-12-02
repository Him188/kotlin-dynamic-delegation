package example.kotlin.compiler.plugin.template.compiler.backend

import example.kotlin.compiler.plugin.template.compiler.config.PluginConfiguration
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.assertCast
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

class PluginIrGenerationExtension(
    private val ext: PluginConfiguration
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        for (file in moduleFragment.files) {
            DynamicDelegationLoweringPass(PluginGenerationContext(pluginContext, ext)).runOnFilePostfix(file)
            RemoveDelegateFieldInitializerPass(pluginContext, ext).runOnFilePostfix(file)
        }
    }
}

data class PluginGenerationContext(
    val context: IrPluginContext,
    val ext: PluginConfiguration,
    val wrapperExpressionMapper: WrapperExpressionMapper = WrapperExpressionMapper(context, ext),
) : IrPluginContext by context

class RemoveDelegateFieldInitializerPass(
    private val context: IrPluginContext,
    private val ext: PluginConfiguration,
) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        val symbols = context.referenceFunctions(DynamicDelegationFqNames.DEFAULT)
        irClass.transformDeclarationsFlat {
            if (it is IrField && it.isDynamicDelegationField(symbols)) {
                it.initializer = null // not removing the field for compatibility
                listOf(it)
            } else null
        }
    }
}

class DynamicDelegationLoweringPass(
    private val pluginContext: PluginGenerationContext,
) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        val dynamicDelegationSymbols = pluginContext.context.referenceFunctions(DynamicDelegationFqNames.DEFAULT)

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
}