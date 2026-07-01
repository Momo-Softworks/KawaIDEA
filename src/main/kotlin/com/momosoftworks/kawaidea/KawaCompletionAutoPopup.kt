package com.momosoftworks.kawaidea

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

/** Schedules member completion immediately after Kawa's Java/member colon syntax. */
class KawaCompletionAutoPopupHandler : TypedHandlerDelegate() {
    /**
     * `afterCharTyped` fires after the character is inserted and the document is
     * updated, so `offset - 1` is the `:` we just typed.  This is the correct
     * hook for on-colon auto-popup; `checkAutoPopup` fires *before* insertion
     * and would see the character before `:`, causing the context check to fail.
     */
    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (c != ':' || file.language != KawaLanguage) return Result.CONTINUE

        PsiDocumentManager.getInstance(project).commitDocument(editor.document)
        if (isKawaMemberColonContext(editor)) {
            AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
        }

        return Result.CONTINUE
    }
}

internal fun isKawaMemberColonContext(editor: Editor): Boolean =
    isKawaMemberColonContext(editor.document.charsSequence, editor.caretModel.offset)

internal fun isKawaMemberColonContext(text: CharSequence, offset: Int): Boolean {
    if (offset <= 0) return false

    val colonOffset = offset - 1
    if (colonOffset !in text.indices || text[colonOffset] != ':') return false
    if (colonOffset > 0 && text[colonOffset - 1] == '#') return false

    var start = colonOffset - 1
    while (start >= 0 && isKawaSymbolPart(text[start])) start--
    val owner = text.subSequence(start + 1, colonOffset).toString()
    if (owner.isEmpty()) return true // receiver shorthand, e.g. :setText

    return (KawaSemantic.isJavaQualifiedName(owner) && KawaSemantic.isLikelyClassName(owner.substringAfterLast('.'))) ||
        (KawaSemantic.isJavaIdentifier(owner) && KawaSemantic.isLikelyClassName(owner))
}

private fun isKawaSymbolPart(ch: Char): Boolean =
    !ch.isWhitespace() && ch !in "()[]{}\"';`,"
