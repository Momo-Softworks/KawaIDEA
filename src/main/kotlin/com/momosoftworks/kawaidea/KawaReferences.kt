package com.momosoftworks.kawaidea

import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.GlobalSearchScope

/**
 * Resolves a fully-qualified class name in Scheme to the Java class. Soft, so a
 * name we can't resolve (deps not indexed, runtime value, typo) doesn't get flagged
 * as an error — this is a navigation aid, not a type checker.
 */
class KawaClassReference(
    element: PsiElement,
    range: TextRange,
    private val fqn: String,
) : PsiReferenceBase<PsiElement>(element, range, /* soft = */ true) {
    override fun resolve(): PsiElement? =
        JavaPsiFacade.getInstance(element.project)
            .findClass(fqn, GlobalSearchScope.allScope(element.project))
}

/**
 * Resolves the member half of `pkg.Class:member` to a method (preferred) or a
 * static/instance field on that class. Also soft.
 */
class KawaMemberReference(
    element: PsiElement,
    range: TextRange,
    private val classFqn: String,
    private val member: String,
) : PsiReferenceBase<PsiElement>(element, range, /* soft = */ true) {
    override fun resolve(): PsiElement? {
        val cls = JavaPsiFacade.getInstance(element.project)
            .findClass(classFqn, GlobalSearchScope.allScope(element.project)) ?: return null
        cls.findMethodsByName(member, true).firstOrNull()?.let { return it }
        return cls.findFieldByName(member, true)
    }
}
