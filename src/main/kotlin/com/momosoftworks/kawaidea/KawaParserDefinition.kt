package com.momosoftworks.kawaidea

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import com.momosoftworks.kawaidea.parser.KawaParser
import com.momosoftworks.kawaidea.psi.KawaFile
import com.momosoftworks.kawaidea.psi.KawaTypes

class KawaParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = KawaLexerAdapter()
    override fun createParser(project: Project?): PsiParser = KawaParser()
    override fun getFileNodeType(): IFileElementType = FILE
    override fun getCommentTokens(): TokenSet = COMMENTS
    override fun getStringLiteralElements(): TokenSet = STRINGS
    override fun getWhitespaceTokens(): TokenSet = WHITE_SPACES
    override fun createElement(node: ASTNode): PsiElement = KawaTypes.Factory.createElement(node)
    override fun createFile(viewProvider: FileViewProvider): PsiFile = KawaFile(viewProvider)

    companion object {
        val FILE = IFileElementType(KawaLanguage)
        val COMMENTS = TokenSet.create(KawaTypes.LINE_COMMENT, KawaTypes.BLOCK_COMMENT)
        val STRINGS = TokenSet.create(KawaTypes.STRING)
        val WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE)
    }
}
