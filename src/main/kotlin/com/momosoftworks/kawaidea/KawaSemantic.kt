package com.momosoftworks.kawaidea

/** Shared, pure Kawa symbol classification used by highlighting, references, and completion. */
enum class ColonKind {
    NONE,
    KEYWORD,
    HASH_KEYWORD,
    JAVA_FQN_MEMBER,
    SHORT_CLASS_MEMBER,
    LEADING_RECEIVER_MEMBER,
    NAMESPACE_OR_NAMED_PART,
    MULTI_COLON_SYMBOL,
}

data class ColonClassification(
    val kind: ColonKind,
    val owner: String?,
    val member: String?,
    val colonIndex: Int,
)

data class ClassAlias(
    val alias: String,
    val qualifiedName: String,
    val sourceText: String,
)

object KawaSemantic {
    fun classifyColonSymbol(text: String): ColonClassification {
        if (text.isEmpty()) return none()
        if (isHashKeyword(text)) return ColonClassification(ColonKind.HASH_KEYWORD, null, text.drop(2), 1)

        val colonCount = text.count { it == ':' }
        if (colonCount == 0) return none()
        if (colonCount > 1) return ColonClassification(ColonKind.MULTI_COLON_SYMBOL, null, null, text.indexOf(':'))

        val colon = text.indexOf(':')
        if (colon == text.lastIndex && colon > 0) {
            return ColonClassification(ColonKind.KEYWORD, null, text.substring(0, colon), colon)
        }
        if (colon == 0 && text.length > 1) {
            return ColonClassification(ColonKind.LEADING_RECEIVER_MEMBER, null, text.substring(1), colon)
        }
        if (colon <= 0 || colon >= text.lastIndex) return none()

        val owner = text.substring(0, colon)
        val member = text.substring(colon + 1)
        val kind = when {
            isJavaQualifiedName(owner) && isLikelyClassName(owner.substringAfterLast('.')) -> ColonKind.JAVA_FQN_MEMBER
            isJavaIdentifier(owner) && isLikelyClassName(owner) -> ColonKind.SHORT_CLASS_MEMBER
            else -> ColonKind.NAMESPACE_OR_NAMED_PART
        }
        return ColonClassification(kind, owner, member, colon)
    }

    fun isKeyword(text: String): Boolean =
        text.length > 1 && text.endsWith(':') && text.count { it == ':' } == 1 && !text.startsWith(':')

    fun isHashKeyword(text: String): Boolean =
        text.length > 2 && text.startsWith("#:") && ':' !in text.drop(2)

    fun isJavaIdentifier(text: String): Boolean {
        if (text.isEmpty()) return false
        val first = text.first()
        if (!(first.isLetter() || first == '_' || first == '$')) return false
        return text.drop(1).all { it.isLetterOrDigit() || it == '_' || it == '$' }
    }

    fun isJavaQualifiedName(text: String): Boolean {
        val parts = text.split('.')
        return parts.size >= 2 && parts.all(::isJavaIdentifier)
    }

    fun isLikelyClassName(text: String): Boolean =
        text.firstOrNull()?.isUpperCase() == true || '$' in text

    fun splitQualifiedMember(text: String): Pair<String, String>? {
        val c = classifyColonSymbol(text)
        return when (c.kind) {
            ColonKind.JAVA_FQN_MEMBER, ColonKind.SHORT_CLASS_MEMBER -> c.owner!! to c.member!!
            else -> null
        }
    }

    fun extractClassAliasesFromImportClassAtoms(atoms: List<String>): List<ClassAlias> {
        if (atoms.size < 3 || atoms.first() != "class") return emptyList()
        val pkg = atoms[1].trimEnd('.')
        val result = mutableListOf<ClassAlias>()
        var i = 2
        while (i < atoms.size) {
            val className = atoms[i]
            if (!isLikelyClassName(className) || !isJavaIdentifier(className)) {
                i++
                continue
            }
            val fqn = "$pkg.$className"
            result += ClassAlias(className, fqn, className)
            val alias = atoms.getOrNull(i + 1)
            if (alias != null && isJavaIdentifier(alias) && !isLikelyClassName(alias)) {
                result += ClassAlias(alias, fqn, alias)
                i += 2
            } else {
                i++
            }
        }
        return result
    }

    private fun none() = ColonClassification(ColonKind.NONE, null, null, -1)
}
