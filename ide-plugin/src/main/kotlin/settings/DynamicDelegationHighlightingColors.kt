package me.him188.kotlin.dynamic.delegation.idea.settings

import com.intellij.openapi.editor.colors.TextAttributesKey
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingColors

object DynamicDelegationHighlightingColors {
    @JvmField
    val DYNAMIC_DELEGATION =
        TextAttributesKey.createTextAttributesKey("DYNAMIC_DELEGATION", KotlinHighlightingColors.SMART_CAST_VALUE)

    @JvmField
    val PERSISTENT = TextAttributesKey.createTextAttributesKey("PERSISTENT", KotlinHighlightingColors.SMART_CAST_VALUE)
}