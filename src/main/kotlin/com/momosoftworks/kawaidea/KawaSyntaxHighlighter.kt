package com.momosoftworks.kawaidea

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.momosoftworks.kawaidea.psi.KawaTypes

/**
 * Lexer-level highlighting (M1). Context-free: it colors one token at a time.
 * Structure-aware coloring (special forms, `Class:method` interop) is layered on
 * top by [KawaAnnotator] (M2), which can see the PSI tree this lexer cannot.
 */
class KawaSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = KawaLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
        when (tokenType) {
            KawaTypes.LINE_COMMENT, KawaTypes.BLOCK_COMMENT -> arrayOf(KawaColors.COMMENT)
            KawaTypes.STRING -> arrayOf(KawaColors.STRING)
            KawaTypes.NUMBER -> arrayOf(KawaColors.NUMBER)
            KawaTypes.BOOLEAN, KawaTypes.CHARACTER -> arrayOf(KawaColors.CONSTANT)
            KawaTypes.HASH_BANG_KEYWORD -> arrayOf(KawaColors.KEYWORD)
            KawaTypes.LPAREN, KawaTypes.RPAREN,
            KawaTypes.LBRACKET, KawaTypes.RBRACKET,
            KawaTypes.HASH_LPAREN -> arrayOf(KawaColors.PARENS)
            else -> EMPTY
        }

    companion object {
        private val EMPTY = emptyArray<TextAttributesKey>()
    }
}
