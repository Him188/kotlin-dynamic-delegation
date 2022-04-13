package me.him188.kotlin.dynamic.delegation.idea.settings

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import me.him188.kotlin.dynamic.delegation.idea.settings.DynamicDelegationHighlightingColors.DYNAMIC_DELEGATION
import me.him188.kotlin.dynamic.delegation.idea.settings.DynamicDelegationHighlightingColors.PERSISTENT
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlighter
import javax.swing.Icon

class HighlightingColorPage : ColorSettingsPage {
    private companion object {
        @Language("kt")
        val DEMO_TEXT = """
            interface I
            class C : I by dynamicDelegation(object : I {})
            
            fun foo(): Int = persistent { 1 }
    """.trimIndent()

        private val attributes: Array<AttributesDescriptor> = arrayOf(
            AttributesDescriptor("Dynamic delegation keyword 'by'", DYNAMIC_DELEGATION),
            AttributesDescriptor("Persistent 'persistent'", PERSISTENT),
        )

        private val tags = attributes.associate { it.displayName to it.key }
    }

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = attributes
    override fun getColorDescriptors(): Array<ColorDescriptor> = arrayOf()
    override fun getDisplayName(): String = "Kotlin Dynamic Delegation"
    override fun getIcon(): Icon? = null
    override fun getHighlighter(): SyntaxHighlighter = KotlinHighlighter() // TODO: 16/12/2021 support highlighting 'by'

    override fun getDemoText(): String = DEMO_TEXT
    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = tags
}

//class KotlinHighlighterWithDynamicDelegations : KotlinHighlighter() {
//    override fun getHighlightingLexer(): Lexer = KotlinLexer()
//
//    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
//        if (tokenType != KtTokens.BY_KEYWORD) return super.getTokenHighlights(tokenType)
//        return arrayOf(KotlinHighlightingColors.KEYWORD, BY_KEYWORD)
////        return super.getTokenHighlights(tokenType) + KotlinHighlightingColors.KEYWORD + BY_KEYWORD
//    }
//}