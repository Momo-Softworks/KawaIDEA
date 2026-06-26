package com.momosoftworks.kawaidea

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.momosoftworks.kawaidea.KawaFileType

/**
 * Headless reformat tests for the hybrid indentation (M3, ALIGN_ARGUMENTS = true).
 */
class KawaFormatterTest : BasePlatformTestCase() {

    private fun reformat(before: String): String {
        myFixture.configureByText(KawaFileType, before)
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(myFixture.file)
        }
        // Commit document changes back to PSI text.
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        return myFixture.file.text
            .lines()
            .joinToString("\n") { it.trimEnd() }
    }

    private fun assertReformat(expected: String, before: String) {
        val expectedNorm = expected.lines().joinToString("\n") { it.trimEnd() }
        val actual = reformat(before)
        if (expectedNorm != actual) {
            println("--- BEFORE ---")
            println(before)
            println("--- EXPECTED ---")
            println(expectedNorm)
            println("--- ACTUAL ---")
            println(actual)
            println("--- END ---")
        }
        assertEquals(expectedNorm, actual)
    }

    fun testDefineBodyIndentsTwo() {
        assertReformat("(define (f x)\n  (+ x 1))", "(define (f x)\n(+ x 1))")
    }

    fun testLetBodyIndentsTwo() {
        assertReformat("(let ((x 1))\n  (+ x 1))", "(let ((x 1))\n     (+ x 1))")
    }

    fun testSpecialNestingAccumulates() {
        assertReformat("(when a\n  (when b\n    (foo)))", "(when a\n(when b\n(foo)))")
    }

    fun testCallAlignsArgsUnderFirstArg() {
        assertReformat("(foo bar\n     baz)", "(foo bar\n  baz)")
    }

    fun testDataListAlignsUnderFirstElement() {
        assertReformat("((a)\n (b))", "((a)\n        (b))")
    }
}
