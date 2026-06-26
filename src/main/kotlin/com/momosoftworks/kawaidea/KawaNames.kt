package com.momosoftworks.kawaidea

/** Shared recognition of the Java-interop name shapes the lexer keeps as one symbol. */
object KawaNames {
    private val SEGMENT = Regex("[A-Za-z_][A-Za-z0-9_]*")

    /** Dotted name whose last segment is Capitalized, e.g. `net.minecraft.item.Item`. */
    fun isClassFqn(text: String): Boolean {
        if ('.' !in text) return false
        val segments = text.split('.')
        if (segments.size < 2) return false
        if (segments.last().firstOrNull()?.isUpperCase() != true) return false
        return segments.all { it.isNotEmpty() && SEGMENT.matches(it) }
    }

    /**
     * For `pkg.Class:member` (single internal colon, class part is an FQN), returns
     * (classFqn, member, colonIndex). Null for keyword args, leading-colon method
     * access on runtime receivers, and anything whose left part isn't a class FQN.
     */
    fun splitInterop(text: String): Triple<String, String, Int>? {
        if (text.count { it == ':' } != 1) return null
        val i = text.indexOf(':')
        if (i <= 0 || i == text.length - 1) return null
        val classPart = text.substring(0, i)
        if (!isClassFqn(classPart)) return null
        return Triple(classPart, text.substring(i + 1), i)
    }
}
