package com.momosoftworks.kawaidea

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/** M4: Scheme interop FQNs resolve into the Java PSI. */
class KawaReferenceTest : BasePlatformTestCase() {

    private fun resolveAtCaret(scm: String): Any? {
        myFixture.configureByText(KawaFileType, scm)
        val ref = myFixture.getReferenceAtCaretPosition()
        System.err.println("REF for '$scm': $ref")
        return ref?.resolve()
    }

    private val api = "package net.example; public class Api { public static Object callHandler() { return null; } }"
    private val items = "package net.example; public class Items { public static Object stick; }"

    override fun setUp() {
        super.setUp()
        // Ensure the Kawa file type is registered before tests run.
    }

    fun testBareFqnResolvesToClass() {
        myFixture.configureByText(KawaFileType, "(define-simple-class S (net.example.It<caret>em))")
        myFixture.addFileToProject("net/example/Item.java", "package net.example; public class Item {}")
        // Verify Java PSI can find the class at all.
        val cls = com.intellij.psi.JavaPsiFacade.getInstance(project)
            .findClass("net.example.Item", com.intellij.psi.search.GlobalSearchScope.allScope(project))
        System.err.println("DIRECT findClass: $cls")
        val ref = myFixture.getReferenceAtCaretPosition()
        System.err.println("REF: $ref")
        val resolved = ref?.resolve()
        System.err.println("RESOLVED: $resolved (${resolved?.javaClass?.name})")
        assertNotNull("JavaPsiFacade could not find the class either", cls)
        assertNotNull("expected a reference to resolve, got null", resolved)
        assertTrue("expected a PsiClass, got $resolved", resolved is PsiClass)
        assertEquals("net.example.Item", (resolved as PsiClass).qualifiedName)
    }

    fun testInteropClassPartResolves() {
        myFixture.addFileToProject("net/example/Api.java", api)
        val resolved = resolveAtCaret("(net.example.Ap<caret>i:callHandler)")
        System.err.println("RESOLVED: $resolved (${resolved?.javaClass?.name})")
        assertNotNull("expected a reference to resolve, got null", resolved)
        assertTrue("expected a PsiClass, got $resolved", resolved is PsiClass)
    }

    fun testInteropMemberResolvesToMethod() {
        myFixture.addFileToProject("net/example/Api.java", api)
        val resolved = resolveAtCaret("(net.example.Api:callHand<caret>ler)")
        System.err.println("RESOLVED: $resolved (${resolved?.javaClass?.name})")
        assertNotNull("expected a reference to resolve, got null", resolved)
        assertTrue("expected a PsiMethod, got $resolved", resolved is PsiMethod)
        assertEquals("callHandler", (resolved as PsiMethod).name)
    }

    fun testInteropMemberResolvesToField() {
        myFixture.addFileToProject("net/example/Items.java", items)
        val resolved = resolveAtCaret("(recipe! net.example.Items:sti<caret>ck)")
        System.err.println("RESOLVED: $resolved (${resolved?.javaClass?.name})")
        assertNotNull("expected a reference to resolve, got null", resolved)
        assertTrue("expected a PsiField, got $resolved", resolved is PsiField)
        assertEquals("stick", (resolved as PsiField).name)
    }

    fun testKeywordArgIsNotAReference() {
        myFixture.configureByText(KawaFileType, "(item! display-na<caret>me: \"x\")")
        val ref = myFixture.getReferenceAtCaretPosition()
        System.err.println("KEYWORD REF: $ref")
        assertNull(ref)
    }
}
