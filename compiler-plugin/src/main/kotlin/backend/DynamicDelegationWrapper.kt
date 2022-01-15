package me.him188.kotlin.dynamic.delegation.compiler.backend

import me.him188.kotlin.dynamic.delegation.compiler.config.PluginConfiguration
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
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

    // [argumentExpression] can be the lambda inside `dynamicDelegation({})`, however it can also be function references and property references
    fun map(ownerSymbol: IrSimpleFunctionSymbol, argumentExpression: IrExpression): MapResult {
        fun IrBuilderWithScope.irGetProperty(
            property: IrPropertyReference
        ): IrExpression {
            property.getter?.let { getter ->
                return irCall(getter).apply {
                    val instance =
                        irGet(ownerSymbol.owner.dispatchReceiverParameter!!) // instance of the container class

                    extensionReceiver = property.extensionReceiver
                        ?: if (getter.owner.extensionReceiverParameter != null) instance else null // KProperty1

                    dispatchReceiver = property.dispatchReceiver
                        ?: if (getter.owner.dispatchReceiverParameter != null) instance else null // KProperty1
                }
            }
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
                // The [argumentExpression] can't be optimized, e.g. a property whose type is `() -> T`
                // We should introduce a field to store this [argumentExpression]

                val field = irFieldFor(ownerSymbol, ownerSymbol.owner.name.asString() + "\$1", argumentExpression)

                MapResult(
                    irCall(invoke, irGetField(irGet(ownerSymbol.owner.dispatchReceiverParameter!!), field)),
                    field
                )
            }
        }
    }

    private fun DeclarationIrBuilder.irFieldFor(
        ownerSymbol: IrSimpleFunctionSymbol,
        name: String,
        initializer: IrExpression
    ): IrField = context.irFactory.buildField {
        startOffset = ownerSymbol.owner.startOffset
        endOffset = ownerSymbol.owner.endOffset
        this.name = Name.identifier(name)
        type = initializer.type
        origin = DYNAMIC_DELEGATION_WRAPPER
        visibility = DescriptorVisibilities.PRIVATE
    }.apply {
        parent = ownerSymbol.owner.parentAsClass
        this.initializer = pluginContext.createIrBuilder(symbol).run {
            irExprBody(initializer)
        }
    }
}

@Suppress("ClassName")
object DYNAMIC_DELEGATION_WRAPPER : IrDeclarationOriginImpl("DYNAMIC_DELEGATION_WRAPPER", true)
