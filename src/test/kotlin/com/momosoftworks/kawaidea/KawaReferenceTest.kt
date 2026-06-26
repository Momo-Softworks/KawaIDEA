package com.momosoftworks.kawaidea

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/** M4: Scheme interop FQNs resolve into the Java PSI. */
class KawaReferenceTest : BasePlatformTestCase() {

    private fun resolveAtCaret(scm: String): Any? {
        myFixture.configureByText("a.scm", scm)
        return myFixture.getReferenceAtCaretPosition()?.resolve()
    }

    private val api = "package net.example; public class Api { public static Object callHandler() { return null; } }"
    private val items = "package net.example; public class Items { public static Object stick; }"

    fun testBareFqnResolvesToClass() {
        myFixture.addFileToProject("net/example/Item.java", "package net.example; public class Item {}")
        val resolved = resolveAtCaret("(define-simple-class S (net.example.It<caret>em))")
        assertTrue("expected a PsiClass, got $resolved", resolved is PsiClass)
        assertEquals("net.example.Item", (resolved as PsiClass).qualifiedName)
    }

    fun testInteropClassPartResolves() {
        myFixture.addFileToProject("net/example/Api.java", api)
        val resolved = resolveAtCaret("(net.example.Ap<caret>i:callHandler)")
        assertTrue("expected a PsiClass, got $resolved", resolved is PsiClass)
    }

    fun testInteropMemberResolvesToMethod() {
        myFixture.addFileToProject("net/example/Api.java", api)
        val resolved = resolveAtCaret("(net.example.Api:callHand<caret>ler)")
        assertTrue("expected a PsiMethod, got $resolved", resolved is PsiMethod)
        assertEquals("callHandler", (resolved as PsiMethod).name)
    }

    fun testInteropMemberResolvesToField() {
        myFixture.addFileToProject("net/example/Items.java", items)
        val resolved = resolveAtCaret("(recipe! net.example.Items:sti<caret>ck)")
        assertTrue("expected a PsiField, got $resolved", resolved is PsiField)
        assertEquals("stick", (resolved as PsiField).name)
    }

    fun testKeywordArgIsNotAReference() {
        // `display-name:` is a keyword arg, not interop — must produce no reference.
        myFixture.configureByText("a.scm", "(item! display-na<caret>me: \"x\")")
        assertNull(myFixture.getReferenceAtCaretPosition())
    }
}
