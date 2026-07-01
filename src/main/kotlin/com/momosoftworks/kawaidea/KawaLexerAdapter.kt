package com.momosoftworks.kawaidea

import com.intellij.lexer.FlexAdapter
import com.momosoftworks.kawaidea.lexer.KawaLexer

class KawaLexerAdapter : FlexAdapter(KawaLexer(null)) {
    /**
     * Enter the BOF state only when lexing starts at offset 0 (top of file),
     * so the shebang rule fires at most once per file.  Mid-file re-lexes
     * (incremental updates) start in YYINITIAL as normal.
     */
    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        super.start(buffer, startOffset, endOffset, initialState)
        if (startOffset == 0) {
            flex.yybegin(KawaLexer.BOF)
        }
    }
}
