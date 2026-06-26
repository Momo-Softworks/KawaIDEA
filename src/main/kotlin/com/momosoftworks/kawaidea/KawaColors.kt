package com.momosoftworks.kawaidea

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Default
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey

/**
 * Central registry of the plugin's TextAttributesKeys, each layered over a
 * platform default so it inherits sensible colors from any theme. Shared by the
 * lexer-level [KawaSyntaxHighlighter] (M1) and the PSI-level [KawaAnnotator] (M2),
 * and the natural backing for a future Color Settings Page.
 */
object KawaColors {
    // Lexer-level (M1)
    val COMMENT = key("KAWA_COMMENT", Default.LINE_COMMENT)
    val STRING = key("KAWA_STRING", Default.STRING)
    val NUMBER = key("KAWA_NUMBER", Default.NUMBER)
    val CONSTANT = key("KAWA_CONSTANT", Default.CONSTANT)
    val PARENS = key("KAWA_PARENS", Default.PARENTHESES)

    // PSI-level (M2)
    val KEYWORD = key("KAWA_KEYWORD", Default.KEYWORD)
    val DEFINITION = key("KAWA_DEFINITION", Default.FUNCTION_DECLARATION)
    val CLASS_REF = key("KAWA_CLASS_REF", Default.CLASS_REFERENCE)
    val METHOD_CALL = key("KAWA_METHOD_CALL", Default.INSTANCE_METHOD)
    val KEYWORD_ARG = key("KAWA_KEYWORD_ARG", Default.METADATA)

    /** Head of a call whose name ends in `!` (Scheme convention for side effects). */
    val SIDE_EFFECT = key("KAWA_SIDE_EFFECT", Default.INSTANCE_METHOD)

    private fun key(name: String, base: TextAttributesKey): TextAttributesKey =
        createTextAttributesKey(name, base)
}
