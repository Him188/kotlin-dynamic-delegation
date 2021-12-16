package me.him188.kotlin.dynamic.delegation.idea.settings

import com.intellij.openapi.editor.colors.TextAttributesKey
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingColors

object DynamicDelegationHighlightingColors {
    @JvmField
    val BY_KEYWORD = TextAttributesKey.createTextAttributesKey("BY_KEYWORD", KotlinHighlightingColors.SMART_CAST_VALUE)
}