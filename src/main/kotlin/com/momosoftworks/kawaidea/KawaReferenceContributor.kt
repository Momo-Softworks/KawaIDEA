package com.momosoftworks.kawaidea

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext
import com.momosoftworks.kawaidea.psi.KawaTypes

/**
 * M4: makes Java-interop names in Scheme navigable. The lexer keeps
 * `net.minecraft.item.Item` and `pkg.Class:method` as single SYMBOL tokens, so each
 * such token gets one or two references resolving into the Java PSI.
 */
class KawaReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // Target the KawaAtom, not the raw SYMBOL leaf: LeafPsiElement.getReferences()
        // does not consult contributed providers, but ASTWrapperPsiElement does.
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(KawaTypes.ATOM),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext,
                ): Array<PsiReference> = referencesFor(element)
            },
        )
    }

    private fun referencesFor(element: PsiElement): Array<PsiReference> {
        // Only atoms that are a bare symbol (not strings/numbers) carry interop names.
        if (element.firstChild?.node?.elementType != KawaTypes.SYMBOL) return PsiReference.EMPTY_ARRAY
        val text = element.text

        // pkg.Class:member  ->  class part + member part
        KawaNames.splitInterop(text)?.let { (classFqn, member, colon) ->
            return arrayOf(
                KawaClassReference(element, TextRange(0, colon), classFqn),
                KawaMemberReference(element, TextRange(colon + 1, text.length), classFqn, member),
            )
        }

        // bare fully-qualified class name
        if (KawaNames.isClassFqn(text)) {
            return arrayOf(KawaClassReference(element, TextRange(0, text.length), text))
        }

        return PsiReference.EMPTY_ARRAY
    }
}
