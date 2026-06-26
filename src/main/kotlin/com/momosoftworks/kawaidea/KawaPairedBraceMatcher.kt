package com.momosoftworks.kawaidea

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.momosoftworks.kawaidea.psi.KawaTypes

class KawaPairedBraceMatcher : PairedBraceMatcher {
    private val pairs = arrayOf(
        BracePair(KawaTypes.LPAREN, KawaTypes.RPAREN, true),
        BracePair(KawaTypes.LBRACKET, KawaTypes.RBRACKET, true),
    )

    override fun getPairs(): Array<BracePair> = pairs

    override fun isPairedBracesAllowedBeforeType(
        lbraceType: IElementType,
        contextType: IElementType?,
    ): Boolean = true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset
}
