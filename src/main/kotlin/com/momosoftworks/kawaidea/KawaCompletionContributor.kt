package com.momosoftworks.kawaidea

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.momosoftworks.kawaidea.psi.KawaAtom
import com.momosoftworks.kawaidea.psi.KawaFile
import com.momosoftworks.kawaidea.psi.KawaForm
import com.momosoftworks.kawaidea.psi.KawaList
import com.momosoftworks.kawaidea.psi.KawaTypes
import javax.swing.Icon

/**
 * M5: Java class/member completion and Scheme symbol completion.
 *
 * Completion triggers on any Kawa atom.  The lexer keeps dotted names and
 * Class:member as single SYMBOL tokens, so the current atom's text IS the
 * completion prefix.
 */
class KawaCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().inside(KawaFile::class.java),
            KawaCompletionProvider(),
        )
    }
}

private class KawaCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        params: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val position = params.position
        val offset = params.offset

        // ---- determine the prefix ------------------------------------------------
        // If the cursor sits on or just after an ATOM, use that atom's text.
        val atom = ancestorAtom(position) ?: parentAtom(position)
        val prefix = atomPrefix(atom, offset) ?: ""

        // ---- route completion ----------------------------------------------------
        if (':' in prefix && prefix.indexOf(':') < prefix.length) {
            completeJavaMembers(prefix, position, result)
        } else if ('.' in prefix) {
            completeDotPrefix(prefix, position, result)
        } else {
            completeSchemeSymbols(prefix, position, result)
        }
    }

    // -----------------------------------------------------------------
    //  Dot prefix → Java classes & packages
    // -----------------------------------------------------------------

    private fun completeDotPrefix(
        prefix: String,
        position: PsiElement,
        result: CompletionResultSet,
    ) {
        val project = position.project
        val scope = GlobalSearchScope.allScope(project)
        val facade = JavaPsiFacade.getInstance(project)

        // Split into package prefix and class-name prefix.
        val lastDot = prefix.lastIndexOf('.')
        val pkgPrefix = if (lastDot > 0) prefix.substring(0, lastDot) else ""
        val classPrefix = prefix.substring(lastDot + 1)

        // 1. Classes whose short name starts with the class prefix.
        if (classPrefix.isNotEmpty()) {
            // unqualified: search all classes by short name
            for (cls in PsiShortNamesCache.getInstance(project)
                .getClassesByName(classPrefix + "*", scope)) {
                val fqn = cls.qualifiedName ?: continue
                if (pkgPrefix.isEmpty() || fqn.startsWith(pkgPrefix + ".")) {
                    result.addElement(
                        classElement(fqn, cls.isInterface)
                    )
                }
            }
        }

        // 2. Sub-packages of the package prefix (only when pkgPrefix is complete
        //    enough to resolve).
        if (classPrefix.isEmpty() || prefix.endsWith(".")) {
            val pkg = facade.findPackage(pkgPrefix.ifEmpty { "" })
            if (pkg != null) {
                for (sub in pkg.subPackages) {
                    result.addElement(packageElement(sub.qualifiedName))
                }
            } else if (pkgPrefix.isEmpty()) {
                // Offer root-level packages (slow path — limit to common JVM pkgs).
                for (root in listOf("java", "javax", "com", "org", "net", "gnu", "kawa")) {
                    result.addElement(packageElement(root))
                }
            }
        }
    }

    // -----------------------------------------------------------------
    //  Colon prefix → Java members
    // -----------------------------------------------------------------

    private fun completeJavaMembers(
        prefix: String,
        position: PsiElement,
        result: CompletionResultSet,
    ) {
        val project = position.project
        val scope = GlobalSearchScope.allScope(project)
        val facade = JavaPsiFacade.getInstance(project)

        val colonIdx = prefix.indexOf(':')
        val classPart = prefix.substring(0, colonIdx)
        val memberPrefix = prefix.substring(colonIdx + 1)

        // Resolve the class part — try as FQN, then as unqualified short name.
        val cls: PsiClass? = resolveClass(classPart, facade, scope)

        if (cls == null) {
            // Offer matching unqualified class names as candidates so the user
            // can complete the class part before adding a member.
            val candidates = PsiShortNamesCache.getInstance(project)
                .getClassesByName(classPart + "*", scope)
            for (c in candidates.take(30)) {
                val fqn = c.qualifiedName ?: continue
                result.addElement(
                    LookupElementBuilder.create(fqn + ":")
                        .withIcon(classIcon(c.isInterface))
                        .withTypeText("class")
                        .withTailText(" → member")
                )
            }
            return
        }

        // Methods
        for (method in cls.allMethods) {
            if (!prefixMatches(memberPrefix, method.name)) continue
            result.addElement(memberElement(cls, method))
        }

        // Fields
        for (field in cls.allFields) {
            if (!prefixMatches(memberPrefix, field.name)) continue
            result.addElement(fieldElement(cls, field))
        }
    }

    // -----------------------------------------------------------------
    //  Scheme symbols
    // -----------------------------------------------------------------

    private fun completeSchemeSymbols(
        prefix: String,
        position: PsiElement,
        result: CompletionResultSet,
    ) {
        val collected = mutableSetOf<String>()

        // Walk the file's PSI tree to find locally bound names.
        val file = position.containingFile as? KawaFile ?: return
        collectLocalBindings(file, position, collected)

        // R7RS small language + Kawa built-ins
        val builtins = R7RS_BUILTINS + KAWA_BUILTINS + KawaForms.SPECIAL_FORMS

        for (name in (collected + builtins).sorted()) {
            if (!prefixMatches(prefix, name)) continue
            val icon = when {
                name in KawaForms.SPECIAL_FORMS -> AllIcons.Nodes.KeymapOther
                name in KawaForms.DEFINING_FORMS -> AllIcons.Nodes.Function
                collected.contains(name) -> AllIcons.Nodes.Variable
                else -> AllIcons.Nodes.Function
            }
            result.addElement(
                LookupElementBuilder.create(name)
                    .withIcon(icon)
                    .withTypeText(if (collected.contains(name)) "local" else "builtin")
            )
        }
    }

    // -----------------------------------------------------------------
    //  Helpers
    // -----------------------------------------------------------------

    /** Walk up from [position] collecting `define`, `lambda`, and `let` bindings. */
    private fun collectLocalBindings(
        file: KawaFile,
        position: PsiElement,
        collected: MutableSet<String>,
    ) {
        val lists = PsiTreeUtil.collectElementsOfType(file, KawaList::class.java)
        for (list in lists) {
            // Look at top-level and nested lists; only consider those before position.
            if (list.textOffset >= position.textOffset) continue
            val forms = list.formList
            val head = forms.firstOrNull()?.atom?.firstChild?.text ?: continue

            when {
                head in KawaForms.DEFINING_FORMS -> {
                    // (define name ...) or (define (name args) ...)
                    val second = forms.getOrNull(1) ?: continue
                    val nameLeaf = second.atom?.firstChild
                    val name = nameLeaf?.text
                    if (name == null) {
                        second.list?.formList?.firstOrNull()?.atom?.firstChild?.text
                            ?.let { collected.add(it) }
                    } else {
                        collected.add(name)
                    }
                }
                head == "lambda" || head == "case-lambda" -> {
                    // (lambda (args) body)
                    val argList = forms.getOrNull(1)?.list ?: continue
                    for (argForm in argList.formList) {
                        argForm.atom?.firstChild?.text?.let { collected.add(it) }
                    }
                }
                head in listOf("let", "let*", "letrec", "let*-values", "let-values",
                               "fluid-let") -> {
                    val bindings = forms.getOrNull(1)?.list ?: continue
                    for (bindForm in bindings.formList) {
                        val bindList = bindForm.list ?: continue
                        bindList.formList.firstOrNull()?.atom?.firstChild?.text
                            ?.let { collected.add(it) }
                    }
                }
            }
        }
    }

    private fun resolveClass(name: String, facade: JavaPsiFacade, scope: GlobalSearchScope): PsiClass? {
        // 1. Fully qualified
        facade.findClass(name, scope)?.let { return it }
        // 2. Unqualified — search short names cache
        val matches = PsiShortNamesCache.getInstance(facade.project)
            .getClassesByName(name, scope)
        if (matches.size == 1) return matches[0]
        // 3. Try common Java packages
        for (pkg in listOf("java.lang", "java.util", "java.io")) {
            facade.findClass("$pkg.$name", scope)?.let { return it }
        }
        return null
    }

    private fun prefixMatches(prefix: String, name: String): Boolean {
        if (prefix.isEmpty()) return true
        // Case-sensitive prefix match (Java is case-sensitive).
        return name.startsWith(prefix)
    }

    /** The atom that is an ancestor of the PSI element at the cursor. */
    private fun ancestorAtom(position: PsiElement): KawaAtom? =
        PsiTreeUtil.getParentOfType(position, KawaAtom::class.java)

    /**
     * When the cursor is next to (not inside) an atom — e.g. between forms —
     * look for an atom just before the cursor.
     */
    private fun parentAtom(position: PsiElement, state: Int = 0): KawaAtom? {
        val parent = position.parent ?: return null
        if (parent is KawaAtom) return parent
        if (parent is KawaForm) {
            // Return the form's atom; fallback to sibling search.
            return parent.atom ?: (parent.prevSibling as? KawaForm)?.atom
            ?: (parent.parent?.prevSibling as? KawaForm)?.atom
        }
        if (parent is KawaFile || parent is KawaList) {
            val prevForm = position.prevSibling as? KawaForm
            return prevForm?.atom
        }
        // Guard against infinite loop.
        if (state > 10) return null
        return parentAtom(parent, state + 1)
    }

    /**
     * Extract the completion prefix from an atom.
     * If [offset] falls inside the atom, return the text up to that point;
     * otherwise return the entire atom text.
     */
    private fun atomPrefix(atom: KawaAtom?, offset: Int): String? {
        if (atom == null) return null
        val text = atom.text
        val atomStart = atom.textOffset
        val cutoff = (offset - atomStart).coerceIn(0, text.length)
        return text.substring(0, cutoff)
    }

    // -----------------------------------------------------------------
    //  Lookup elements
    // -----------------------------------------------------------------

    private fun classElement(fqn: String, isInterface: Boolean): LookupElementBuilder {
        val simpleName = fqn.substringAfterLast('.')
        val pkg = fqn.substringBeforeLast('.', "")
        return LookupElementBuilder.create(fqn)
            .withPresentableText(simpleName)
            .withIcon(classIcon(isInterface))
            .withTypeText(if (isInterface) "interface" else "class")
            .withTailText("  ($pkg)", true)
    }

    private fun packageElement(fqn: String): LookupElementBuilder =
        LookupElementBuilder.create(fqn)
            .withIcon(AllIcons.Nodes.Package)
            .withTypeText("package")

    private fun memberElement(cls: PsiClass, method: PsiMethod): LookupElementBuilder {
        val static = method.hasModifierProperty(PsiModifier.STATIC)
        val params = method.parameterList.parameters.joinToString(", ") { p ->
            p.type.presentableText
        }
        val returnType = method.returnType?.presentableText ?: "void"

        val displayName = cls.qualifiedName + ":" + method.name
        val presentable = method.name

        return LookupElementBuilder.create(displayName, presentable)
            .withIcon(if (static) AllIcons.Nodes.Method else AllIcons.Nodes.Method)
            .withTypeText(returnType)
            .withTailText("($params)" + if (static) "  static" else "", true)
    }

    private fun fieldElement(cls: PsiClass, field: com.intellij.psi.PsiField): LookupElementBuilder {
        val static = field.hasModifierProperty(PsiModifier.STATIC)
        val type = field.type.presentableText

        val displayName = cls.qualifiedName + ":" + field.name
        val presentable = field.name

        return LookupElementBuilder.create(displayName, presentable)
            .withIcon(if (static) AllIcons.Nodes.Field else AllIcons.Nodes.Field)
            .withTypeText(type)
            .withTailText(if (static) "  static" else "", true)
    }

    private fun classIcon(isInterface: Boolean): Icon =
        if (isInterface) AllIcons.Nodes.Interface else AllIcons.Nodes.Class

    companion object {
        // R7RS small language procedures (as symbols, not special forms).
        val R7RS_BUILTINS = setOf(
            "+", "-", "*", "/", "=", "<", ">", "<=", ">=",
            "abs", "append", "apply", "assoc", "assq", "assv",
            "boolean?", "boolean=?", "bytevector", "bytevector?",
            "caar", "cadr", "car", "cdr", "cddr", "cdar", "caaar",
            "ceiling", "char?", "char->integer", "char-ready?",
            "close-port", "complex?", "cons", "cos", "current-input-port",
            "current-output-port", "current-error-port",
            "display", "eq?", "eqv?", "equal?", "error", "even?",
            "exact?", "exact-integer?", "expt", "features",
            "floor", "flush-output-port", "for-each", "gcd", "get-output-string",
            "inexact?", "input-port?", "integer?", "integer->char",
            "lcm", "length", "list", "list?", "list->string",
            "list->vector", "map", "max", "member", "memq", "memv",
            "min", "modulo", "negative?", "newline", "not", "null?",
            "number?", "number->string", "odd?", "open-input-file",
            "open-input-string", "open-output-file", "open-output-string",
            "output-port?", "pair?", "peek-char", "port?", "positive?",
            "procedure?", "quotient", "rational?", "rationalize",
            "read", "read-char", "read-string", "real?",
            "remainder", "reverse", "round", "sin",
            "sqrt", "string", "string?", "string->list",
            "string->number", "string->symbol", "string->utf8",
            "string-append", "string-copy", "string-length", "string-ref",
            "string-set!", "substring", "symbol?", "symbol->string",
            "tan", "truncate", "utf8->string", "vector", "vector?",
            "vector->list", "vector-append", "vector-copy",
            "vector-length", "vector-ref", "vector-set!", "write",
            "write-char", "zero?",
        )

        val KAWA_BUILTINS = setOf(
            "format", "future", "force", "sleep",
            "invoke", "invoke-static", "invoke-special",
            "instance?", "as", "synchronized",
            "object", "runnable", "this",
            "try-catch", "try-finally", "primitive-throw",
            "define-alias", "define-member-alias",
            "define-namespace", "define-constant",
        )
    }
}
