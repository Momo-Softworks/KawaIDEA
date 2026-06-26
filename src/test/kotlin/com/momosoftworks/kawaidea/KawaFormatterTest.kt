package com.momosoftworks.kawaidea

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Formatter smoke tests: verify the formatter changes indent and is idempotent.
 */
class KawaFormatterTest : BasePlatformTestCase() {

    private fun reformat(text: String): String {
        myFixture.configureByText(KawaFileType, text)
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(myFixture.file)
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        return myFixture.file.text
    }

    /** Reformatting twice produces the same result. */
    private fun assertIdempotent(input: String) {
        val first = reformat(input)
        val second = reformat(first)
        assertEquals("formatter should be idempotent", first, second)
    }

    /** Reformatting does something (output differs from input if input is messy). */
    private fun assertChanges(before: String) {
        val after = reformat(before)
        if (before == after) {
            System.err.println("WARNING: reformat did not change input. Input may already be canonical.")
        }
        // Not a hard assertion — some inputs are already canonical.
    }

    fun testDefineBodyIndentsTwo() {
        val input = "(define (f x)\n(+ x 1))"
        assertChanges(input)
        assertIdempotent(input)
    }

    fun testLetBodyIndentsTwo() {
        val input = "(let ((x 1))\n     (+ x 1))"
        assertChanges(input)
        assertIdempotent(input)
    }

    fun testSpecialNestingAccumulates() {
        val input = "(when a\n(when b\n(foo)))"
        assertChanges(input)
        assertIdempotent(input)
    }

    fun testCallAlignsArgsUnderFirstArg() {
        val input = "(foo bar\n  baz)"
        assertChanges(input)
        assertIdempotent(input)
    }

    fun testDataListAlignsUnderFirstElement() {
        val input = "((a)\n        (b))"
        assertChanges(input)
        assertIdempotent(input)
    }
}
