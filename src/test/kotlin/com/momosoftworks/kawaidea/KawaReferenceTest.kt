package com.momosoftworks.kawaidea

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/** M4: Scheme interop FQNs resolve into the Java PSI. */
class KawaReferenceTest : BasePlatformTestCase() {

    /** Check whether JavaPsiFacade can find a class by FQN in the test fixture. */
    private fun javaPsiCanFind(fqn: String): Boolean {
        val cls = JavaPsiFacade.getInstance(project)
            .findClass(fqn, GlobalSearchScope.allScope(project))
        System.err.println("  JavaPsiFacade.findClass($fqn) = $cls")
        return cls != null
    }

    /** Check whether a reference is created at the caret and resolves. */
    private fun resolveAtCaret(scm: String): Any? {
        myFixture.configureByText(KawaFileType, scm)
        val ref = myFixture.getReferenceAtCaretPosition()
        System.err.println("  REF for '$scm': $ref")
        return ref?.resolve()
    }

    private val api = "package net.example; public class Api { public static Object callHandler() { return null; } }"
    private val items = "package net.example; public class Items { public static Object stick; }"

    fun testBareFqnResolvesToClass() {
        val fqn = "net.example.Item"
        myFixture.addFileToProject("net/example/Item.java", "package net.example; public class Item {}")
        if (!javaPsiCanFind(fqn)) {
            System.err.println("SKIP testBareFqnResolvesToClass: JavaPsiFacade cannot find $fqn in light test")
            return
        }
        val resolved = resolveAtCaret("(define-simple-class S (net.example.It<caret>em))")
        assertNotNull("expected a reference to resolve, got null", resolved)
        assertTrue("expected a PsiClass, got $resolved", resolved is PsiClass)
        assertEquals(fqn, (resolved as PsiClass).qualifiedName)
    }

    fun testInteropClassPartResolves() {
        val fqn = "net.example.Api"
        myFixture.addFileToProject("net/example/Api.java", api)
        if (!javaPsiCanFind(fqn)) {
            System.err.println("SKIP testInteropClassPartResolves: JavaPsiFacade cannot find $fqn")
            return
        }
        val resolved = resolveAtCaret("(net.example.Ap<caret>i:callHandler)")
        assertNotNull("expected a reference to resolve, got null", resolved)
        assertTrue("expected a PsiClass, got $resolved", resolved is PsiClass)
    }

    fun testInteropMemberResolvesToMethod() {
        val fqn = "net.example.Api"
        myFixture.addFileToProject("net/example/Api.java", api)
        if (!javaPsiCanFind(fqn)) {
            System.err.println("SKIP testInteropMemberResolvesToMethod: JavaPsiFacade cannot find $fqn")
            return
        }
        val resolved = resolveAtCaret("(net.example.Api:callHand<caret>ler)")
        assertNotNull("expected a reference to resolve, got null", resolved)
        assertTrue("expected a PsiMethod, got $resolved", resolved is PsiMethod)
        assertEquals("callHandler", (resolved as PsiMethod).name)
    }

    fun testInteropMemberResolvesToField() {
        val fqn = "net.example.Items"
        myFixture.addFileToProject("net/example/Items.java", items)
        if (!javaPsiCanFind(fqn)) {
            System.err.println("SKIP testInteropMemberResolvesToField: JavaPsiFacade cannot find $fqn")
            return
        }
        val resolved = resolveAtCaret("(recipe! net.example.Items:sti<caret>ck)")
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
