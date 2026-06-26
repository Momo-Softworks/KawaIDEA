package com.momosoftworks.kawaidea

import com.intellij.openapi.command.WriteCommandAction
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
        myFixture.configureByText("a.scm", before)
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(myFixture.file)
        }
        return myFixture.file.text
    }

    private fun assertReformat(expected: String, before: String) {
        val actual = reformat(before)
        // Print both so CI logs show the diff when it fails.
        println("BEFORE:  ${before.replace("\n", "\\n")}")
        println("ACTUAL:  ${actual.replace("\n", "\\n")}")
        println("EXPECT:  ${expected.replace("\n", "\\n")}")
        assertEquals(expected, actual)
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
