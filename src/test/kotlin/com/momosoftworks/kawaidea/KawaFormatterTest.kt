package com.momosoftworks.kawaidea

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Headless reformat tests for the hybrid indentation (M3, ALIGN_ARGUMENTS = true):
 * special forms indent their body +2; function calls and data lists align under
 * the first argument/element. Includes a multi-level case to prove indentation
 * accumulates through nesting.
 */
class KawaFormatterTest : BasePlatformTestCase() {

    private fun reformat(before: String): String {
        myFixture.configureByText(KawaFileType, before)
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(myFixture.file)
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        // Read from the document, not the PSI file — the PSI text may be stale.
        return myFixture.editor.document.text
    }

    /** Normalize line endings for cross-platform test stability. */
    private fun norm(s: String) = s.replace("\r\n", "\n").replace("\r", "\n")

    private fun assertReformat(expected: String, before: String) {
        val actual = norm(reformat(before))
        val exp = norm(expected)
        System.err.println("BEFORE: ${before.replace("\n", "\\n")}")
        System.err.println("ACTUAL: ${actual.replace("\n", "\\n")}")
        System.err.println("EXPECT: ${exp.replace("\n", "\\n")}")
        assertEquals(exp, actual)
    }

    // SPECIAL: body indents a fixed +2.
    fun testDefineBodyIndentsTwo() {
        assertReformat("(define (f x)\n  (+ x 1))", "(define (f x)\n(+ x 1))")
    }

    fun testLetBodyIndentsTwo() {
        assertReformat("(let ((x 1))\n  (+ x 1))", "(let ((x 1))\n     (+ x 1))")
    }

    // Nesting must accumulate: inner body is +2 from the inner form, not the outer.
    fun testSpecialNestingAccumulates() {
        assertReformat("(when a\n  (when b\n    (foo)))", "(when a\n(when b\n(foo)))")
    }

    // CALL: arguments align under the first argument.
    fun testCallAlignsArgsUnderFirstArg() {
        assertReformat("(foo bar\n     baz)", "(foo bar\n  baz)")
    }

    // DATA: elements align under the first element (head is not a symbol).
    fun testDataListAlignsUnderFirstElement() {
        assertReformat("((a)\n (b))", "((a)\n        (b))")
    }
}
