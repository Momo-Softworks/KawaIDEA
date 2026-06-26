package com.momosoftworks.kawaidea.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry

/**
 * Base class for [KawaAtom] impls (wired via `mixin` in Kawa.bnf). The generated
 * ASTWrapperPsiElement.getReferences() does NOT consult contributed reference
 * providers, so we delegate to the registry here — this is what makes the M4
 * Scheme→Java references in KawaReferenceContributor actually attach.
 */
abstract class KawaAtomMixin(node: ASTNode) : ASTWrapperPsiElement(node) {
    override fun getReferences(): Array<PsiReference> =
        ReferenceProvidersRegistry.getReferencesFromProviders(this)
}
