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

    // SPECIAL: body indents a fixed +2.
    fun testDefineBodyIndentsTwo() {
        assertEquals("(define (f x)\n  (+ x 1))", reformat("(define (f x)\n(+ x 1))"))
    }

    fun testLetBodyIndentsTwo() {
        assertEquals("(let ((x 1))\n  (+ x 1))", reformat("(let ((x 1))\n     (+ x 1))"))
    }

    // Nesting must accumulate: inner body is +2 from the inner form, not the outer.
    fun testSpecialNestingAccumulates() {
        assertEquals(
            "(when a\n  (when b\n    (foo)))",
            reformat("(when a\n(when b\n(foo)))"),
        )
    }

    // CALL: arguments align under the first argument.
    fun testCallAlignsArgsUnderFirstArg() {
        assertEquals("(foo bar\n     baz)", reformat("(foo bar\n  baz)"))
    }

    // DATA: elements align under the first element (head is not a symbol).
    fun testDataListAlignsUnderFirstElement() {
        assertEquals("((a)\n (b))", reformat("((a)\n        (b))"))
    }
}
