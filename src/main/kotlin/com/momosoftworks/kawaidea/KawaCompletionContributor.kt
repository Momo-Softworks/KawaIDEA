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

// =====================================================================
//  Public routing logic — extracted for testability.
// =====================================================================

/** How a completion prefix should be handled. */
enum class CompletionMode {
    /** pkg.Class: — Java member completion. */
    JAVA_MEMBER,
    /** pkg.Class or pkg. — Java class/package completion. */
    DOT_PREFIX,
    /** Plain symbol — Scheme builtins + locals + Java unqualified classes. */
    SCHEME_SYMBOL,
}

/**
 * Classify a completion prefix into the correct [CompletionMode].
 * Visible for testing.
 */
fun classifyPrefix(prefix: String): CompletionMode {
    if (':' in prefix) {
        if (prefix.endsWith(':') && !prefix.startsWith("#:") && prefix.length > 1) {
            val owner = prefix.dropLast(1)
            if ((KawaSemantic.isJavaQualifiedName(owner) && KawaSemantic.isLikelyClassName(owner.substringAfterLast('.'))) ||
                (KawaSemantic.isJavaIdentifier(owner) && KawaSemantic.isLikelyClassName(owner))
            ) {
                return CompletionMode.JAVA_MEMBER
            }
        }
        return when (KawaSemantic.classifyColonSymbol(prefix).kind) {
            ColonKind.JAVA_FQN_MEMBER,
            ColonKind.SHORT_CLASS_MEMBER,
            ColonKind.LEADING_RECEIVER_MEMBER -> CompletionMode.JAVA_MEMBER
            else -> CompletionMode.SCHEME_SYMBOL
        }
    }
    return if ('.' in prefix) CompletionMode.DOT_PREFIX else CompletionMode.SCHEME_SYMBOL
}

/**
 * Find Java class short names matching [prefix] via [PsiShortNamesCache].
 * Does NOT use `getClassesByName(name + "*")` — that method needs an exact
 * short name.  Instead we scan `getAllClassNames()` and filter.
 *
 * Visible for testing.
 */
fun matchingClassShortNames(prefix: String, project: com.intellij.openapi.project.Project): List<String> {
    if (prefix.length < 2) return emptyList()
    val cache = PsiShortNamesCache.getInstance(project)
    return cache.allClassNames
        .filter { it.startsWith(prefix) }
        .take(50)
        .toList()
}

// =====================================================================
//  Provider
// =====================================================================

private class KawaCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        params: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val position = params.position
        val offset = params.offset

        // -- determine the prefix -----------------------------------------------
        val atom = ancestorAtom(position) ?: parentAtom(position)
        val prefix = atomPrefix(atom, offset) ?: ""

        // -- context-sensitive override ----------------------------------------
        // Some Kawa forms expect Java class names in specific argument positions,
        // regardless of whether the prefix contains dots or colons.
        val contextHead = contextListHead(position)
        if (contextHead in CLASS_CONTEXT_FORMS && prefix.isNotEmpty()) {
            // Force class completion for e.g. (invoke-static |, (instance? obj |, etc.
            completeDotPrefix(prefix, position, result)
            completeSchemeSymbols(prefix, position, result)
            return
        }

        // -- route --------------------------------------------------------------
        when (classifyPrefix(prefix)) {
            CompletionMode.JAVA_MEMBER  -> completeJavaMembers(prefix, position, result)
            CompletionMode.DOT_PREFIX   -> completeDotPrefix(prefix, position, result)
            CompletionMode.SCHEME_SYMBOL -> completeSchemeSymbols(prefix, position, result)
        }
    }

    /**
     * Return the head symbol of the nearest parent list, or null.
     * E.g. for `(invoke-static |` returns `"invoke-static"`.
     */
    private fun contextListHead(position: PsiElement): String? {
        var parent = position.parent
        for (i in 1..5) {
            if (parent == null) return null
            if (parent is KawaList) {
                return parent.formList.firstOrNull()?.atom?.firstChild?.text
            }
            parent = parent.parent
        }
        return null
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
        val endsWithDot = prefix.endsWith(".")

        // 1. Classes whose short name matches, filtered by package.
        //    When the prefix ends with a dot, show ALL classes in that package.
        if (classPrefix.isNotEmpty() || endsWithDot) {
            val allClasses = if (endsWithDot) {
                // Prefix ends with dot — get classes in the specific package.
                val pkg = facade.findPackage(pkgPrefix)
                if (pkg != null) {
                    pkg.getClasses(scope).toList()
                } else {
                    // Package not indexed (e.g. build-directory classes).
                    // Fall back: search all classes with this FQN prefix.
                    facade.findClasses(pkgPrefix, scope).toList()
                }
            } else {
                // Non-empty class prefix — use getAllClassNames (exact API, no wildcards).
                val cache = PsiShortNamesCache.getInstance(project)
                cache.allClassNames
                    .filter { it.startsWith(classPrefix) }
                    .take(60)
                    .mapNotNull { name ->
                        val classes = cache.getClassesByName(name, scope)
                        classes.firstOrNull()
                    }
            }
            for (cls in allClasses) {
                val fqn = cls.qualifiedName ?: continue
                if (pkgPrefix.isEmpty() || fqn.startsWith(pkgPrefix + ".")) {
                    result.addElement(classElement(fqn, cls.isInterface))
                }
            }
        }

        // 2. Sub-packages of the package prefix.
        if (classPrefix.isEmpty() || endsWithDot) {
            val pkg = facade.findPackage(pkgPrefix.ifEmpty { "" })
            if (pkg != null) {
                for (sub in pkg.subPackages) {
                    result.addElement(packageElement(sub.qualifiedName))
                }
            } else if (pkgPrefix.isEmpty()) {
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
            // Offer matching unqualified class names so the user can complete
            // the class part before picking a member.
            val cache = PsiShortNamesCache.getInstance(project)
            val matching = cache.allClassNames
                .filter { it.startsWith(classPart) }
                .take(30)
            for (shortName in matching) {
                val candidates = cache.getClassesByName(shortName, scope)
                for (c in candidates) {
                    val fqn = c.qualifiedName ?: continue
                    result.addElement(
                        LookupElementBuilder.create(fqn + ":")
                            .withPresentableText(shortName)
                            .withIcon(classIcon(c.isInterface))
                            .withTypeText("class")
                            .withTailText(" → member")
                    )
                }
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
    //  Scheme symbols + unqualified Java classes
    // -----------------------------------------------------------------

    private fun completeSchemeSymbols(
        prefix: String,
        position: PsiElement,
        result: CompletionResultSet,
    ) {
        val project = position.project
        val scope = GlobalSearchScope.allScope(project)
        val collected = mutableSetOf<String>()

        // Walk the file's PSI tree to find locally bound names.
        val file = position.containingFile as? KawaFile
        if (file != null) {
            collectLocalBindings(file, position, collected)
        }

        // ---- unqualified Java classes (first — fast path) ------------------
        // Limit iteration so this completes before the timeout, unlike the
        // full allClassNames scan which can be slow with thousands of entries.
        if (prefix.length >= 2) {
            val cache = PsiShortNamesCache.getInstance(project)
            var added = 0
            for (shortName in cache.allClassNames) {
                if (!shortName.startsWith(prefix)) continue
                val classes = cache.getClassesByName(shortName, scope)
                for (cls in classes) {
                    val fqn = cls.qualifiedName ?: continue
                    val pkg = fqn.substringBeforeLast('.', "")
                    result.addElement(
                        LookupElementBuilder.create(shortName)
                            .withIcon(classIcon(cls.isInterface))
                            .withTypeText(if (cls.isInterface) "interface" else "class")
                            .withTailText("  ($pkg)", true)
                    )
                    added++
                    if (added >= 30) break  // hard limit to stay responsive
                }
                if (added >= 30) break
            }
        }

        // ---- cross-project Scheme symbols ----------------------------------
        val projectBindings = KawaProjectCache.getInstance(project).allDefinedSymbols()
        for (name in projectBindings) {
            if (!prefixMatches(prefix, name)) continue
            if (name in collected) continue
            result.addElement(
                LookupElementBuilder.create(name)
                    .withIcon(AllIcons.Nodes.Variable)
                    .withTypeText("project")
            )
        }

        // ---- builtins -------------------------------------------------------
        val builtins = R7RS_BUILTINS + KAWA_BUILTINS + KawaForms.SPECIAL_FORMS

        // Templates (snippets) — shown before builtins so they surface first.
        for ((name, skeleton) in KAWA_TEMPLATES) {
            if (!prefixMatches(prefix, name)) continue
            result.addElement(
                LookupElementBuilder.create(name)
                    .withIcon(AllIcons.Nodes.Template)
                    .withTypeText("snippet")
                    .withTailText("  $skeleton", true)
            )
        }

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

    private fun collectLocalBindings(
        file: KawaFile,
        position: PsiElement,
        collected: MutableSet<String>,
    ) {
        val lists = PsiTreeUtil.collectElementsOfType(file, KawaList::class.java)
        for (list in lists) {
            if (list.textOffset >= position.textOffset) continue
            val forms = list.formList
            val head = forms.firstOrNull()?.atom?.firstChild?.text ?: continue

            when {
                head in KawaForms.DEFINING_FORMS -> {
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

    /**
     * Resolve a possibly-unqualified class name.  Tries:
     * 1. Fully-qualified name
     * 2. Exact short-name match
     * 3. Common Java packages (java.lang, java.util, java.io)
     * 4. If the name is 2+ chars, prefix search via getAllClassNames
     */
    private fun resolveClass(name: String, facade: JavaPsiFacade, scope: GlobalSearchScope): PsiClass? {
        // 1. Fully qualified
        facade.findClass(name, scope)?.let { return it }
        // 2. Unqualified — exact short name match
        val cache = PsiShortNamesCache.getInstance(facade.project)
        val exactMatches = cache.getClassesByName(name, scope)
        if (exactMatches.size == 1) return exactMatches[0]
        // 3. Common Java packages
        for (pkg in listOf("java.lang", "java.util", "java.io")) {
            facade.findClass("$pkg.$name", scope)?.let { return it }
        }
        // 4. Partial / prefix match via getAllClassNames
        if (name.length >= 2) {
            val matching = cache.allClassNames
                .filter { it.startsWith(name) }
                .take(5)
            for (shortName in matching) {
                val candidates = cache.getClassesByName(shortName, scope)
                if (candidates.isNotEmpty()) return candidates[0]
            }
        }
        return null
    }

    private fun prefixMatches(prefix: String, name: String): Boolean {
        if (prefix.isEmpty()) return true
        return name.startsWith(prefix)
    }

    private fun ancestorAtom(position: PsiElement): KawaAtom? =
        PsiTreeUtil.getParentOfType(position, KawaAtom::class.java)

    private fun parentAtom(position: PsiElement, state: Int = 0): KawaAtom? {
        val parent = position.parent ?: return null
        if (parent is KawaAtom) return parent
        if (parent is KawaForm) {
            return parent.atom ?: (parent.prevSibling as? KawaForm)?.atom
            ?: (parent.parent?.prevSibling as? KawaForm)?.atom
        }
        if (parent is KawaFile || parent is KawaList) {
            val prevForm = position.prevSibling as? KawaForm
            return prevForm?.atom
        }
        if (state > 10) return null
        return parentAtom(parent, state + 1)
    }

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
        /** Kawa special forms where argument positions expect Java class names. */
        val CLASS_CONTEXT_FORMS = setOf(
            "invoke-static", "invoke-special",
            "instance?", "as",
            "define-simple-class", "define-class",
        )

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

        /** Templates inserted as snippets when selected (tail text shows the skeleton). */
        val KAWA_TEMPLATES = mapOf(
            "define-simple-class" to "(define-simple-class Name (Super) ...)",
            "define-mod" to "(define-mod \"id\" name: \"Name\" version: \"0.1.0\")",
            "invoke-static" to "(invoke-static Class:method arg ...)",
            "try-catch" to "(try-catch (begin ...) (ex Exception (begin ...)))",
            "synchronized" to "(synchronized lock ...)",
        )
    }
}
