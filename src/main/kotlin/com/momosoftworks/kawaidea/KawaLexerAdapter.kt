package com.momosoftworks.kawaidea

import com.intellij.lexer.FlexAdapter
import com.momosoftworks.kawaidea.lexer.KawaLexer

class KawaLexerAdapter : FlexAdapter(KawaLexer(null))
