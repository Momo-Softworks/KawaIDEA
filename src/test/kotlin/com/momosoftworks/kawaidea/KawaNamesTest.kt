package com.momosoftworks.kawaidea

import org.junit.Assert.*
import org.junit.Test

/**
 * Pure unit tests for [KawaNames] — no IntelliJ runtime needed.
 */
class KawaNamesTest {

    @Test
    fun `isClassFqn - valid fully-qualified class names`() {
        assertTrue(KawaNames.isClassFqn("net.minecraft.item.Item"))
        assertTrue(KawaNames.isClassFqn("java.lang.String"))
        assertTrue(KawaNames.isClassFqn("cpw.mods.fml.common.registry.GameRegistry"))
        assertTrue(KawaNames.isClassFqn("com.momosoftworks.Example"))
    }

    @Test
    fun `isClassFqn - rejects non-class patterns`() {
        assertFalse(KawaNames.isClassFqn("display"))             // no dot
        assertFalse(KawaNames.isClassFqn("java.lang.string"))    // last segment lowercase
        assertFalse(KawaNames.isClassFqn("java."))               // trailing dot
        assertFalse(KawaNames.isClassFqn(".String"))             // leading dot
        assertFalse(KawaNames.isClassFqn("String"))              // no dot
        assertFalse(KawaNames.isClassFqn("pkg.Class:method"))    // has colon
        // "a.b.C" is actually valid — single-letter package names are legal.
        assertTrue(KawaNames.isClassFqn("a.b.C"))
    }

    @Test
    fun `isClassFqn - edge cases`() {
        assertFalse(KawaNames.isClassFqn(""))
        assertFalse(KawaNames.isClassFqn("."))
        assertFalse(KawaNames.isClassFqn(".."))
        assertTrue(KawaNames.isClassFqn("com.example.MyClass_1"))
        assertTrue(KawaNames.isClassFqn("net.minecraft.item.ItemStack"))
    }

    @Test
    fun `splitInterop - valid Class colon member patterns`() {
        val result = KawaNames.splitInterop("java.lang.String:valueOf")
        assertNotNull(result)
        assertEquals("java.lang.String", result!!.first)
        assertEquals("valueOf", result.second)
        assertEquals(16, result.third) // colon position
    }

    @Test
    fun `splitInterop - rejects non-interop patterns`() {
        assertNull(KawaNames.splitInterop("display"))           // no colon
        assertNull(KawaNames.splitInterop(":method"))           // leading colon
        assertNull(KawaNames.splitInterop("name:"))             // trailing colon (keyword)
        assertNull(KawaNames.splitInterop("a:b:c"))             // multiple colons
        assertNull(KawaNames.splitInterop("notaclass:method"))  // left side not FQN class
    }

    @Test
    fun `splitInterop - single-segment class with colon`() {
        // "String:valueOf" — String alone is not an FQN (no dot), so splitInterop rejects it
        assertNull(KawaNames.splitInterop("String:valueOf"))
    }
}
