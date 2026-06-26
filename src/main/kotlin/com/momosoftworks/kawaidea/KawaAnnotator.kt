package com.momosoftworks.kawaidea

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.momosoftworks.kawaidea.psi.KawaForm
import com.momosoftworks.kawaidea.psi.KawaList
import com.momosoftworks.kawaidea.psi.KawaTypes

/**
 * Structure-aware (PSI-level) highlighting — M2. Two independent concerns:
 *  - on a [KawaList], color the head symbol if it is a special form, and the
 *    declared name in a defining form (positional, needs the tree);
 *  - on a SYMBOL leaf, color interop syntax (`Class:method`), keyword arguments
 *    (`name:`), and bare fully-qualified class names (text patterns, no position).
 * The two paths never touch the same range, so there is no double-annotation.
 */
class KawaAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when {
            element is KawaList -> annotateList(element, holder)
            element.node?.elementType == KawaTypes.SYMBOL -> annotateSymbol(element, holder)
        }
    }

    private fun annotateList(list: KawaList, holder: AnnotationHolder) {
        val forms = list.formList
        val headLeaf = forms.firstOrNull()?.symbolLeaf() ?: return
        val head = headLeaf.text

        when {
            head in KawaForms.SPECIAL_FORMS -> {
                mark(holder, headLeaf.textRange, KawaColors.KEYWORD)
                if (head in KawaForms.DEFINING_FORMS) {
                    definedNameLeaf(forms)?.let { mark(holder, it.textRange, KawaColors.DEFINITION) }
                }
            }
            // Bang procedures: `nbt-set!`, `msg!`, `item!`, ... (set! is a special form above).
            head.length > 1 && head.endsWith("!") ->
                mark(holder, headLeaf.textRange, KawaColors.SIDE_EFFECT)
        }
    }

    private fun annotateSymbol(symbol: PsiElement, holder: AnnotationHolder) {
        val text = symbol.text
        val start = symbol.textRange.startOffset
        val colons = text.count { it == ':' }

        when {
            // keyword argument label: `name:` `version:`
            colons == 1 && text.length > 1 && text.endsWith(":") ->
                mark(holder, symbol.textRange, KawaColors.KEYWORD_ARG)

            // leading-colon method access: `:setUnlocalizedName`
            colons == 1 && text.length > 1 && text.startsWith(":") ->
                mark(holder, symbol.textRange, KawaColors.METHOD_CALL)

            // interop `pkg.Class:method` -> split into class part + method part
            colons == 1 -> {
                val i = text.indexOf(':')
                mark(holder, TextRange(start, start + i), KawaColors.CLASS_REF)
                mark(holder, TextRange(start + i + 1, symbol.textRange.endOffset), KawaColors.METHOD_CALL)
            }

            // bare fully-qualified class name: `net.minecraft.item.Item`
            KawaNames.isClassFqn(text) -> mark(holder, symbol.textRange, KawaColors.CLASS_REF)
        }
    }

    private fun mark(holder: AnnotationHolder, range: TextRange, key: TextAttributesKey) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(range)
            .textAttributes(key)
            .create()
    }

    /** The SYMBOL leaf of a form, if the form is a bare symbol atom. */
    private fun KawaForm.symbolLeaf(): PsiElement? {
        val leaf = atom?.firstChild ?: return null
        return leaf.takeIf { it.node?.elementType == KawaTypes.SYMBOL }
    }

    /**
     * The name a defining form declares: the 2nd form when it's a bare symbol
     * (`(define foo ...)`, `(define-simple-class Foo ...)`), or the head of the
     * 2nd form when it's a list (`(define (foo x) ...)`).
     */
    private fun definedNameLeaf(forms: List<KawaForm>): PsiElement? {
        val second = forms.getOrNull(1) ?: return null
        second.symbolLeaf()?.let { return it }
        return second.list?.formList?.firstOrNull()?.symbolLeaf()
    }

}
