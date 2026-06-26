package com.momosoftworks.kawaidea

import org.junit.Assert.*
import org.junit.Test

/**
 * Pure unit tests for completion routing logic — no IntelliJ runtime needed.
 */
class KawaCompletionLogicTest {

    @Test
    fun `classifyPrefix - Java member completion`() {
        assertEquals(CompletionMode.JAVA_MEMBER, classifyPrefix("String:val"))
        assertEquals(CompletionMode.JAVA_MEMBER, classifyPrefix("java.lang.String:value"))
        assertEquals(CompletionMode.JAVA_MEMBER, classifyPrefix("net.minecraft.item.Item:"))
        assertEquals(CompletionMode.JAVA_MEMBER, classifyPrefix("GameRegistry:reg"))
    }

    @Test
    fun `classifyPrefix - dot prefix completion`() {
        assertEquals(CompletionMode.DOT_PREFIX, classifyPrefix("java.lang"))
        assertEquals(CompletionMode.DOT_PREFIX, classifyPrefix("net.minecraft.item.Item"))
        assertEquals(CompletionMode.DOT_PREFIX, classifyPrefix("net."))
        assertEquals(CompletionMode.DOT_PREFIX, classifyPrefix("java.lang.St"))
        assertEquals(CompletionMode.DOT_PREFIX, classifyPrefix("net.minecraft.item."))
    }

    @Test
    fun `classifyPrefix - scheme symbol completion`() {
        assertEquals(CompletionMode.SCHEME_SYMBOL, classifyPrefix("disp"))
        assertEquals(CompletionMode.SCHEME_SYMBOL, classifyPrefix("Game"))
        assertEquals(CompletionMode.SCHEME_SYMBOL, classifyPrefix("defi"))
        assertEquals(CompletionMode.SCHEME_SYMBOL, classifyPrefix(""))
        assertEquals(CompletionMode.SCHEME_SYMBOL, classifyPrefix("invoke-st"))
        assertEquals(CompletionMode.SCHEME_SYMBOL, classifyPrefix("name:"))
        assertEquals(CompletionMode.SCHEME_SYMBOL, classifyPrefix("#:name"))
        assertEquals(CompletionMode.SCHEME_SYMBOL, classifyPrefix("foo:bar"))
    }

    @Test
    fun `classifyPrefix - colon priority over dot`() {
        // pkg.Class:member — colon takes priority for member completion
        assertEquals(CompletionMode.JAVA_MEMBER,
            classifyPrefix("com.example.Foo:bar"))
    }

    @Test
    fun `classifyPrefix - trailing colon means no member prefix yet`() {
        // "String:" — colon is present and there's nothing after it yet.
        // The routing says ':' in prefix AND colon position < length (it is at the end,
        // so indexOf(':') == length-1, which IS < length). So it routes to JAVA_MEMBER.
        // The member completion code will handle the empty member prefix gracefully.
        assertEquals(CompletionMode.JAVA_MEMBER, classifyPrefix("String:"))
    }
}
