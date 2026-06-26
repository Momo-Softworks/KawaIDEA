package com.momosoftworks.kawaidea

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Reproduces "delete the indent before a line, let it snap back" — the on-type
 * path (adjustLineIndent / getChildAttributes), which is separate from full
 * reformat. The new indent must match where reformat puts the same line.
 */
class KawaReindentTest : BasePlatformTestCase() {

    private val messy = """
        (define carved-stick
        (item! "p"
        on-right-click: (lambda (x)
        (when c
        (let ((uses 0))
        (nbt-set! stack "uses" uses)
        (msg! player "x"))))))
    """.trimIndent()

    private fun reindentMatchesReformat(marker: String) {
        myFixture.configureByText("a.scm", messy)
        val doc = myFixture.editor.document
        val csm = CodeStyleManager.getInstance(project)

        WriteCommandAction.runWriteCommandAction(project) {
            csm.reformat(myFixture.file)
        }
        val lineIdx = doc.text.lines().indexOfFirst { it.contains(marker) }
        val expected = doc.text.lines()[lineIdx].takeWhile { it == ' ' }.length

        WriteCommandAction.runWriteCommandAction(project) {
            val start = doc.getLineStartOffset(lineIdx)
            var end = start
            while (doc.charsSequence[end] == ' ') end++
            doc.deleteString(start, end)
            PsiDocumentManager.getInstance(project).commitDocument(doc)
            csm.adjustLineIndent(myFixture.file, doc.getLineStartOffset(lineIdx))
        }
        val actual = doc.text.lines()[lineIdx].takeWhile { it == ' ' }.length
        assertEquals("re-indent of '$marker' (reformat=$expected)", expected, actual)
    }

    fun testReindentNbtSet() = reindentMatchesReformat("nbt-set!")
    fun testReindentMsg() = reindentMatchesReformat("msg!")
}
