package com.momosoftworks.kawaidea

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KawaSemanticTest {
    @Test
    fun `classifies Kawa keyword forms`() {
        val keyword = KawaSemantic.classifyColonSymbol("name:")
        assertEquals(ColonKind.KEYWORD, keyword.kind)
        assertEquals("name", keyword.member)
        assertNull(keyword.owner)

        val hashKeyword = KawaSemantic.classifyColonSymbol("#:name")
        assertEquals(ColonKind.HASH_KEYWORD, hashKeyword.kind)
        assertEquals("name", hashKeyword.member)
        assertNull(hashKeyword.owner)
    }

    @Test
    fun `classifies Java member forms separately from namespaces`() {
        assertEquals(ColonKind.JAVA_FQN_MEMBER, KawaSemantic.classifyColonSymbol("java.lang.String:valueOf").kind)
        assertEquals(ColonKind.SHORT_CLASS_MEMBER, KawaSemantic.classifyColonSymbol("String:valueOf").kind)
        assertEquals(ColonKind.LEADING_RECEIVER_MEMBER, KawaSemantic.classifyColonSymbol(":setText").kind)
        assertEquals(ColonKind.NAMESPACE_OR_NAMED_PART, KawaSemantic.classifyColonSymbol("foo:bar").kind)
        assertEquals(ColonKind.MULTI_COLON_SYMBOL, KawaSemantic.classifyColonSymbol("a:b:c").kind)
        assertEquals(ColonKind.NONE, KawaSemantic.classifyColonSymbol("display").kind)
    }

    @Test
    fun `recognizes JVM identifiers and qualified names`() {
        assertTrue(KawaSemantic.isJavaIdentifier("String"))
        assertTrue(KawaSemantic.isJavaIdentifier("Map\$Entry"))
        assertFalse(KawaSemantic.isJavaIdentifier("1String"))
        assertTrue(KawaSemantic.isJavaQualifiedName("java.util.Map"))
        assertFalse(KawaSemantic.isJavaQualifiedName("java."))
        assertTrue(KawaSemantic.isLikelyClassName("String"))
        assertFalse(KawaSemantic.isLikelyClassName("string"))
    }

    @Test
    fun `extracts class aliases from import class atoms`() {
        val aliases = KawaSemantic.extractClassAliasesFromImportClassAtoms(
            listOf("class", "java.util", "Map", "HashMap", "hmap")
        )
        assertEquals(
            listOf(
                ClassAlias("Map", "java.util.Map", "Map"),
                ClassAlias("HashMap", "java.util.HashMap", "HashMap"),
                ClassAlias("hmap", "java.util.HashMap", "hmap"),
            ),
            aliases,
        )
    }

    @Test
    fun `detects member colon contexts for autopopup`() {
        assertTrue(isKawaMemberColonContext("String:", "String:".length))
        assertTrue(isKawaMemberColonContext("java.lang.String:", "java.lang.String:".length))
        assertTrue(isKawaMemberColonContext(":", 1))
        assertFalse(isKawaMemberColonContext("name:", "name:".length))
        assertFalse(isKawaMemberColonContext("foo:bar", 4))
        assertFalse(isKawaMemberColonContext("#:name", 2))
    }

    @Test
    fun `detects member prefixes after deletion`() {
        assertTrue(isKawaMemberCompletionPrefix("String:", "String:".length))
        assertTrue(isKawaMemberCompletionPrefix("String:val", "String:val".length))
        assertTrue(isKawaMemberCompletionPrefix("(java.lang.String:val", "(java.lang.String:val".length))
        assertFalse(isKawaMemberCompletionPrefix("name:", "name:".length))
        assertFalse(isKawaMemberCompletionPrefix("foo:bar", "foo:bar".length))
    }
}
