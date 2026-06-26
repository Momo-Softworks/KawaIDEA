package com.momosoftworks.kawaidea

/**
 * Knowledge of Kawa/Scheme special forms, kept as plain data so it is trivial to
 * extend. M2 (this annotator) uses it to color keywords; M3's indentation table
 * will be the same shape (symbol -> indent rule), so treat this as the seed of a
 * shared "what kind of form is this?" registry.
 */
object KawaForms {

    /** Head symbols that should be highlighted as keywords. */
    val SPECIAL_FORMS: Set<String> = setOf(
        // Core Scheme binding/control
        "define", "define-values", "define-syntax", "define-record-type",
        "lambda", "case-lambda",
        "let", "let*", "letrec", "letrec*", "let-values", "let*-values", "fluid-let",
        "if", "when", "unless", "cond", "case", "and", "or", "begin",
        "set!", "quote", "quasiquote", "unquote", "unquote-splicing",
        "do", "delay", "delay-force", "make-promise", "parameterize",
        "guard", "syntax-rules", "syntax-case", "with-syntax",
        // Modules
        "module-name", "import", "require", "include", "export", "library", "define-library",
        // Kawa-specific
        "define-simple-class", "define-class", "define-alias", "define-namespace",
        "define-constant", "define-private", "define-macro", "invoke", "invoke-static",
        "invoke-special", "as", "instance?", "field", "static-field", "this", "try-catch",
        "try-finally", "synchronized",
        // Project macros (extend as KawaCraft/Patina add more)
        "define-mod",
    )

    /**
     * Defining forms whose second element names something we should highlight as
     * a declaration (e.g. the `foo` in `(define foo ...)`).
     */
    val DEFINING_FORMS: Set<String> = setOf(
        "define", "define-values", "define-syntax", "define-record-type",
        "define-simple-class", "define-class", "define-alias", "define-constant",
        "define-private", "define-macro", "define-namespace", "define-mod",
    )

    /**
     * Forms whose body indents a fixed +2 (the SPECIAL mode in [KawaBlock]):
     * defun-like, binding, and control forms. A head NOT in this set is treated
     * as a function call (align args under the first argument).
     *
     * v1 simplification: this models Emacs's "indent body by 2" but NOT the full
     * "N distinguished arguments" rule. It is correct for define/let/lambda/when;
     * the visible cost is that `cond`/`case` clauses indent +2 rather than
     * aligning under the first clause. Refining that (per-form distinguished
     * counts) is the M3.1 follow-up — change this Set into a Map<String, Int>.
     */
    val BODY_INDENT_FORMS: Set<String> = setOf(
        "define", "define-values", "define-syntax", "define-record-type",
        "lambda", "case-lambda",
        "let", "let*", "letrec", "letrec*", "let-values", "let*-values", "fluid-let",
        "if", "when", "unless", "cond", "case", "begin", "do",
        "parameterize", "guard", "syntax-rules", "syntax-case", "with-syntax",
        "define-simple-class", "define-class", "define-macro", "define-private",
        "define-mod", "try-catch", "try-finally", "synchronized",
    )
}
