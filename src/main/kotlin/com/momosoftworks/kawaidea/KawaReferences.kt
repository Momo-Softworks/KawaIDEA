package com.momosoftworks.kawaidea

import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache

private val COMMON_JAVA_PACKAGES = listOf("java.lang", "java.util", "java.io")

internal fun resolveKawaJavaClass(project: com.intellij.openapi.project.Project, name: String): PsiClass? {
    val scope = GlobalSearchScope.allScope(project)
    val facade = JavaPsiFacade.getInstance(project)

    facade.findClass(name, scope)?.let { return it }

    if ('.' !in name) {
        for (pkg in COMMON_JAVA_PACKAGES) {
            facade.findClass("$pkg.$name", scope)?.let { return it }
        }
        PsiShortNamesCache.getInstance(project).getClassesByName(name, scope).firstOrNull()?.let { return it }
    }

    return null
}

/**
 * Resolves a class name in Scheme to the Java class. Soft, so a name we can't
 * resolve doesn't get flagged as an error — this is navigation aid, not type checking.
 */
class KawaClassReference(
    element: PsiElement,
    range: TextRange,
    private val className: String,
) : PsiReferenceBase<PsiElement>(element, range, /* soft = */ true) {
    override fun resolve(): PsiElement? = resolveKawaJavaClass(element.project, className)
}

/**
 * Resolves the member half of `Class:member` to a method (preferred) or field.
 * Overload/type/static checks are intentionally left for a later semantic pass.
 */
class KawaMemberReference(
    element: PsiElement,
    range: TextRange,
    private val className: String,
    private val member: String,
) : PsiReferenceBase<PsiElement>(element, range, /* soft = */ true) {
    override fun resolve(): PsiElement? {
        val cls = resolveKawaJavaClass(element.project, className) ?: return null
        cls.findMethodsByName(member, true).firstOrNull()?.let { return it }
        return cls.findFieldByName(member, true)
    }
}
