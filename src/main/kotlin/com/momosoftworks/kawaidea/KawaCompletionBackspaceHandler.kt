package com.momosoftworks.kawaidea

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

/** Keeps Kawa member completion visible when deleting within `Class:member` text. */
class KawaCompletionBackspaceHandler : BackspaceHandlerDelegate() {
    override fun beforeCharDeleted(c: Char, file: PsiFile, editor: Editor) = Unit

    override fun charDeleted(c: Char, file: PsiFile, editor: Editor): Boolean {
        if (file.language == KawaLanguage && isKawaMemberCompletionPrefix(editor.document.charsSequence, editor.caretModel.offset)) {
            val project: Project = file.project
            AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
        }
        return false
    }
}

internal fun isKawaMemberCompletionPrefix(text: CharSequence, offset: Int): Boolean {
    if (offset < 0 || offset > text.length) return false
    var start = offset - 1
    while (start >= 0 && !text[start].isWhitespace() && text[start] !in "()[]{}\"'`,;") start--
    val prefix = text.subSequence(start + 1, offset).toString()
    return classifyPrefix(prefix) == CompletionMode.JAVA_MEMBER
}
