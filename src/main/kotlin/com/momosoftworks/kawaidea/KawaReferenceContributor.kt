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

        // Class:member  ->  class part + member part. Use the shared classifier so
        // namespace-like `foo:bar` and keyword forms do not get Java references.
        val classification = KawaSemantic.classifyColonSymbol(text)
        if (classification.kind == ColonKind.JAVA_FQN_MEMBER || classification.kind == ColonKind.SHORT_CLASS_MEMBER) {
            val className = classification.owner ?: return PsiReference.EMPTY_ARRAY
            val member = classification.member ?: return PsiReference.EMPTY_ARRAY
            val colon = classification.colonIndex
            return arrayOf(
                KawaClassReference(element, TextRange(0, colon), className),
                KawaMemberReference(element, TextRange(colon + 1, text.length), className, member),
            )
        }

        // bare fully-qualified class name
        if (KawaNames.isClassFqn(text) || KawaSemantic.isJavaQualifiedName(text)) {
            return arrayOf(KawaClassReference(element, TextRange(0, text.length), text))
        }

        return PsiReference.EMPTY_ARRAY
    }
}
