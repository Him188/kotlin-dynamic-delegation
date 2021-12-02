package example.kotlin.compiler.plugin.template.compiler.backend

import example.kotlin.compiler.plugin.template.compiler.config.PluginConfiguration
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.Name

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
object DYNAMIC_DELEGATION_WRAPPER : IrDeclarationOriginImpl("DYNAMIC_DELEGATION_WRAPPER", true)
