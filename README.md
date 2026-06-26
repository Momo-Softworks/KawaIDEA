# KawaIDEA

A native IntelliJ Platform language plugin for **Kawa** (JVM Scheme), built to
make editing `.scm` and `.sld` files for KawaCraft / Patina feel alive: real
highlighting, Emacs‑style indentation, Scheme→Java navigation, and completion.

## Why a native plugin (not a TextMate grammar or LSP)

TextMate‑based "Scheme" plugins are regex highlighters with no model of the
code, so they can't do indentation or completion. An LSP server can't reach
IntelliJ's Java PSI, so it can't give real Scheme→Java navigation. A native
plugin can do all of it — and Scheme is trivial to parse (atoms + lists), so
the usual hard part of a language plugin is nearly free here.

## Implemented MVP features

- **File type** registration for `.scm` and `.sld`.
- **Lexer / parser / PSI** generated from JFlex and Grammar‑Kit.
- **Syntax highlighting** and brace matching.
- **Annotator** for special forms, defined names, keyword arguments, and
  `Class:method` interop.
- **Formatter** with 2‑space indentation (Lisp‑style).
- **Reference resolution** (`PsiReference`) that links Scheme symbols to Java
  classes and members.
- **Completion** offering in‑file symbols and Java member suggestions.
- **Structure view** for quick navigation of top‑level forms.
- **REPL run configuration** allowing you to launch a Kawa REPL directly from
  the IDE.

## First‑class Kawa roadmap (post‑MVP)

| Item | Description |
|------|-------------|
| Imports / modules / exports stubs | Basic support for `import`, `module`, and `export` forms. |
| Richer lexical scopes | Precise scope handling for let‑bindings, definitions, and macros. |
| Java member overloads / types | Resolve overloaded methods and provide type‑aware completions. |
| Run / compile configurations | More flexible execution and compilation options in the IDE. |
| Inspections | Detect common mistakes (unused vars, mismatched arities, etc.). |
| Documentation provider | Show Kawa and Java doc pop‑ups for symbols. |
| Macro expansion | Visualize macro-expanded code and support macro‑aware navigation. |

## Build & run (IntelliJ target 2026.1 / build 261.*)

The project is configured to target **IntelliJ IDEA 2026.1** (sinceBuild `261`,
untilBuild `261.*`).

Requires JDK 17 or 21 (set one as the Gradle JVM). The first build will download
the target IDE, so the initial run is slower.

```bash
./gradlew generateLexer generateParser   # codegen into src/main/gen (also runs automatically)
./gradlew buildPlugin                     # produces build/distributions/KawaIDEA-*.zip
./gradlew runIde                          # launches a sandbox IDE with the plugin loaded
```

In IntelliJ: open this folder as a Gradle project, then use the **Run Plugin**
(`runIde`) configuration. Open a `.scm` or `.sld` file – you should see
highlighting, brace matching, PSI view, structure view, and the REPL run
configuration.

### Test notes
- Tests are executed in the Guix environment with `JAVA_HOME` set appropriately
  (e.g., `JAVA_HOME=/gnu/store/...-openjdk-21.0.2-jdk`).
- The current pure‑test helper (`run-tests.sh`) runs JUnit4 tests without requiring
  an IDE.

### Things to confirm
- The plugin registers the `.scm` and `.sld` extensions. Disable any other
  Scheme plugin to avoid file‑type conflicts.
- Ensure the `build.gradle.kts` `ideaVersion` block matches the target IDE version
  (2026.1 / build 261.*).
