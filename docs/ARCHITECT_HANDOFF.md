# KawaIDEA Architect Handoff

This document is for the next architect/worker session continuing KawaIDEA. It summarizes current state, verified commands, recent work, and recommended next goals so the next session does not need to reread prior chat logs.

## Current repository state

Repository: `git@github.com:Momo-Softworks/KawaIDEA.git`

Main branch has recently received two MVP commits:

- `29d86ed Improve Kawa semantic interop MVP`
- `56ea33a Auto-popup Kawa member completion`

The plugin targets IntelliJ IDEA `2026.1` / build `261.*` in `build.gradle.kts`.

Local-only architect runtime artifacts may exist and should usually remain uncommitted:

- `.architect/`
- `plan.yaml`
- `plan.yaml.state.json`

If a future architect wants to preserve architect planning artifacts, first decide whether they belong in the repo; they are currently treated as local scratch/session state.

## Verification commands

Local Guix environment command that has passed repeatedly:

```bash
JAVA_HOME=/gnu/store/369mpiwwzq5hi60nd0nmiz9x2slv4py5-openjdk-21.0.2-jdk \
  ./gradlew --no-daemon --no-configuration-cache generateLexer compileKotlin compileTestKotlin

JAVA_HOME=/gnu/store/369mpiwwzq5hi60nd0nmiz9x2slv4py5-openjdk-21.0.2-jdk \
  ./run-tests.sh
```

`run-tests.sh` currently runs pure/non-IDE JUnit tests:

- `KawaNamesTest`
- `KawaSemanticTest`
- `KawaCompletionLogicTest`
- `KawaFormsTest`

The full Gradle `test` task may fail locally on Guix/JBR/AWT native-library issues. GitHub Actions is configured to run `./gradlew test --info` on Ubuntu and should be the better signal for IntelliJ fixture tests.

## Recently completed work

### Shared Kawa semantic classifier

File: `src/main/kotlin/com/momosoftworks/kawaidea/KawaSemantic.kt`

Adds pure classification for Kawa colon-bearing symbols:

- `name:` -> `KEYWORD`
- `#:name` -> `HASH_KEYWORD`
- `java.lang.String:valueOf` -> `JAVA_FQN_MEMBER`
- `String:valueOf` -> `SHORT_CLASS_MEMBER`
- `:setText` -> `LEADING_RECEIVER_MEMBER`
- `foo:bar` -> `NAMESPACE_OR_NAMED_PART`
- `a:b:c` -> `MULTI_COLON_SYMBOL`

The classifier is intentionally pure Kotlin, with no IntelliJ PSI dependency, so it is easy to test.

### Consistent colon behavior across features

Updated:

- `KawaAnnotator.kt`
- `KawaCompletionContributor.kt`
- `KawaReferenceContributor.kt`
- `KawaReferences.kt`

Current behavior:

- Java-looking colon symbols get Java highlighting/completion/references.
- Keyword and namespace-like symbols no longer get treated as Java interop.
- Short class references like `String:valueOf` can resolve through common packages / short-name search.

### Completion auto-popup improvements

Added:

- `KawaCompletionAutoPopup.kt`
- `KawaCompletionBackspaceHandler.kt`

Registered in `plugin.xml`:

- `typedHandler`
- `backspaceHandlerDelegate`

Current behavior:

- Typing `:` after Java/member contexts schedules completion immediately.
- Backspacing inside a member prefix schedules completion again if the caret remains in a Java/member completion prefix.
- It avoids triggering for normal Kawa keywords/namespaces such as `name:`, `#:name`, and `foo:bar`.

### Project symbol completion freshness

Updated:

- `KawaProjectCache.kt`

It now invalidates on unsaved Kawa document edits through the editor event multicaster, not just saved/VFS file changes. Completion should therefore see project Kawa definitions from source without requiring compilation and should refresh as the user types.

### Lexer tweaks

Updated:

- `src/main/grammars/Kawa.flex`

Current additions:

- shebang lines lex as line comments,
- `#;` datum comment marker is tokenized as a symbol marker rather than bad-character noise,
- `#:foo` remains a symbol.

Note: full datum-comment semantics are not implemented; the parser still sees `#;` as a form rather than skipping the following datum.

### Structure/formatter polish

Updated:

- `KawaBlock.kt`
- `KawaStructureViewFactory.kt`

Current behavior:

- formatter keeps fixed 2-space Lisp indentation but now centralizes the constant and documents the choice,
- structure-view items navigate to the defining form offset rather than just opening the file.

## Important known limitations

1. **No real import/module/export semantic model yet.**
   `import`, `require`, `module-name`, `define-library`, exports, and `(import (class ...))` are not fully resolved as lexical bindings.

2. **Project symbol cache is still a cache, not a stub/index.**
   It scans Kawa source files and top-level definitions. This is acceptable for MVP but should become stubs/indexes for serious projects.

3. **Lexical scoping is approximate.**
   Completion collects definitions/locals by scanning lists before the caret. It does not yet model proper lexical scope boundaries or macro-introduced bindings.

4. **Java member resolution is name-only.**
   Overloads, static-vs-instance constraints, receiver type inference, bean property aliases, and Kawa name mangling are not yet fully handled.

5. **Macro support is minimal.**
   Built-in/syntax forms are listed, but there is no macro expansion service or syntax binding model.

6. **Run support is REPL-only.**
   There is no first-class run-current-file, compile, or generated-class run configuration yet.

7. **Full Kawa lexical coverage is incomplete.**
   Nested block comments, true datum-comment skipping, case-folding directives, vertical-bar escaped identifiers, and richer Kawa reader forms need more work.

## Recommended next architect goals

The next architect should prefer small, mechanically verifiable goals. Suggested order:

### Goal 1: Import/class alias extraction from PSI

Deliverables:

- add pure-ish helpers around `KawaList` to recognize `(import (class ...))`,
- collect class aliases such as `(import (class java.util Map (HashMap HMap)))`,
- add tests for alias extraction.

Acceptance:

- compile + pure tests pass,
- `HMap:` can classify/resolve as `java.util.HashMap:` in completion/reference contexts.

### Goal 2: Source module/export model MVP

Deliverables:

- semantic helpers for `module-name`, `define-library`, `export`, `module-export`, `define-private`,
- update project symbol completion to know exported/private names.

Acceptance:

- pure tests for module/export extraction,
- completion does not offer private exports as project-level public symbols if export filtering is implemented.

### Goal 3: Replace/augment `KawaProjectCache` with an index-like layer

Deliverables:

- decide FileBasedIndex vs stubs,
- index top-level definitions/module names/exports from Kawa files,
- keep document-change freshness for currently open unsaved files.

Acceptance:

- project-wide completion remains source-based and does not require compilation,
- compile/tests pass,
- performance does not depend on full PSI scans on every completion.

### Goal 4: Type-position and receiver-member completion

Deliverables:

- recognize explicit Kawa type annotations (`::`, simple `x::Type` forms),
- resolve standard Kawa types and Java class aliases,
- use obvious receiver type annotations to complete `receiver:` members.

Acceptance:

- tests for type-ref parsing/class resolution,
- manual IDE test: typed variable with Java class type offers relevant members.

### Goal 5: Run/compile configurations

Deliverables:

- run current Kawa source file,
- compile current Kawa file with `kawa -C`,
- generated-class run mode if `--main`/`module-compile-options main: #t` is detected.

Acceptance:

- configuration classes compile,
- context producer can create a run config from a `.scm` file.

## Suggested worker/architect workflow

Use the architect skill only for decomposing, freezing seams, and verifying. Avoid broad worker goals like “implement imports.” Good leaf goals should touch 1-2 files and have a pure test or compile command.

Prefer pure tests for semantic helpers:

- `KawaSemanticTest.kt`
- `KawaCompletionLogicTest.kt`
- new pure test classes for import/module/type extraction where possible.

Use IntelliJ fixture tests only when necessary; local Guix environment may make full fixture execution noisy.

## Push/CI notes

GitHub CI workflow: `.github/workflows/build.yml`

It runs on push to `main`:

1. setup JDK 21,
2. `./gradlew generateLexer generateParser`,
3. `./gradlew test --info`,
4. upload test reports,
5. `./gradlew buildPlugin`,
6. upload plugin artifact.

The most recent push command used successfully:

```bash
git -C /home/swilley/Projects/KawaIDEA push origin main
```

If SSH push initially fails with public-key issues, retry once; the Bitwarden SSH agent may need a moment.
