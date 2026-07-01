package com.momosoftworks.kawaidea.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;
import com.momosoftworks.kawaidea.psi.KawaTypes;

%%

%public
%class KawaLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType

WHITE_SPACE=[ \t\f\r\n]+
LINE_COMMENT=";"[^\r\n]*
// Kawa special markers: #!null, #!void, #!key, #!rest, #!optional, etc.
// The first char after #! is always a letter for these.
HASH_BANG_KEYWORD="#!"[a-zA-Z][a-zA-Z0-9_?-]*
// Unix shebang: #!/usr/bin/env kawa — first char after #! is always '/',
// never a letter.  We enforce that here so SHEBANG can never match longer
// than HASH_BANG_KEYWORD on the same input (JFlex picks the longest match,
// not the first rule — order only breaks ties).
SHEBANG="#!"[^a-zA-Z\r\n][^\r\n]*
DATUM_COMMENT_MARKER="#;"
BLOCK_COMMENT="#|"~"|#"

STRING=\"([^\\\"]|\\[^])*\"?
BOOLEAN="#t"|"#f"|"#true"|"#false"
CHARACTER="#\\"([a-zA-Z][a-zA-Z0-9-]*|.)
NUMBER=[+-]?[0-9][0-9a-fA-F.eE+\-/x]*

// A Scheme symbol is any run of characters that are not whitespace, list
// delimiters, string/quote markers, or a comment start. This deliberately
// includes ':', '.', '-', '>', etc. so interop names like
// com.momosoftworks.kawacraft.KawaCraftAPI:callHandler lex as ONE symbol.
// Updated to support keyword symbols like #:foo and datum comment marker #;.
SYMBOL=[^ \t\f\r\n()\[\]{}\"';`,]+

%%

<YYINITIAL> {
  {WHITE_SPACE}           { return TokenType.WHITE_SPACE; }
  {HASH_BANG_KEYWORD}     { return KawaTypes.HASH_BANG_KEYWORD; }
  {SHEBANG}               { return KawaTypes.LINE_COMMENT; }
  {LINE_COMMENT}     { return KawaTypes.LINE_COMMENT; }
  {DATUM_COMMENT_MARKER} { return KawaTypes.SYMBOL; }
  {BLOCK_COMMENT}    { return KawaTypes.BLOCK_COMMENT; }

  "#("               { return KawaTypes.HASH_LPAREN; }
  "("                { return KawaTypes.LPAREN; }
  ")"                { return KawaTypes.RPAREN; }
  "["                { return KawaTypes.LBRACKET; }
  "]"                { return KawaTypes.RBRACKET; }

  ",@"               { return KawaTypes.UNQUOTE_SPLICING; }
  "'"                { return KawaTypes.QUOTE; }
  "`"                { return KawaTypes.QUASIQUOTE; }
  ","                { return KawaTypes.UNQUOTE; }

  {STRING}           { return KawaTypes.STRING; }
  {BOOLEAN}          { return KawaTypes.BOOLEAN; }
  {CHARACTER}        { return KawaTypes.CHARACTER; }
  {NUMBER}           { return KawaTypes.NUMBER; }
  {SYMBOL}           { return KawaTypes.SYMBOL; }
}

[^]                  { return TokenType.BAD_CHARACTER; }
