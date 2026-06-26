package com.momosoftworks.kawaidea

import org.junit.Assert.*
import org.junit.Test

/**
 * Consistency checks for [KawaForms] data sets.
 */
class KawaFormsTest {

    @Test
    fun `DEFINING_FORMS is subset of SPECIAL_FORMS`() {
        for (form in KawaForms.DEFINING_FORMS) {
            assertTrue("'$form' in DEFINING_FORMS but not SPECIAL_FORMS",
                form in KawaForms.SPECIAL_FORMS)
        }
    }

    @Test
    fun `BODY_INDENT_FORMS is subset of SPECIAL_FORMS`() {
        // Not all special forms use body indent, but the intent is that
        // BODY_INDENT_FORMS ⊆ SPECIAL_FORMS.
        for (form in KawaForms.BODY_INDENT_FORMS) {
            assertTrue("'$form' in BODY_INDENT_FORMS but not SPECIAL_FORMS",
                form in KawaForms.SPECIAL_FORMS)
        }
    }

    @Test
    fun `key Scheme forms are present`() {
        val required = setOf("define", "lambda", "if", "let", "quote", "set!")
        for (r in required) {
            assertTrue("Missing core form: $r", r in KawaForms.SPECIAL_FORMS)
        }
    }

    @Test
    fun `key Kawa forms are present`() {
        val required = setOf("define-simple-class", "invoke", "invoke-static",
            "instance?", "try-catch", "module-name", "define-library")
        for (r in required) {
            assertTrue("Missing Kawa form: $r", r in KawaForms.SPECIAL_FORMS)
        }
    }
}
