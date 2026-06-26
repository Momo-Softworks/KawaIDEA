# KawaIDEA

A native IntelliJ Platform language plugin for **Kawa** (JVM Scheme), built to
make editing `.scm` files for KawaCraft / Patina feel alive: real highlighting,
Emacs-style indentation, Scheme→Java navigation, and completion.

## Why a native plugin (not a TextMate grammar or LSP)

TextMate-based "Scheme" plugins are regex highlighters with no model of the
code, so they can't do indentation or completion. An LSP server can't reach
IntelliJ's Java PSI, so it can't give real Scheme→Java navigation. A native
plugin can do all of it — and Scheme is trivial to parse (atoms + lists), so
the usual hard part of a language plugin is nearly free here.

## Roadmap

| Milestone | Delivers | Status |
|-----------|----------|--------|
| **M0** | Plugin skeleton, `.scm` registered | done |
| **M1** | Lexer + parser → PSI, brace matching, basic highlighting | done (this scaffold) |
| **M2** | Annotator: special forms, defined names, `Class:method` interop, keyword args, FQNs | done |
| **M3** | Formatter: SPECIAL (+2 body) / CALL / DATA indent modes, 2-space default, reformat tests | done |
| **M4** | `PsiReference` resolving interop FQNs into Java PSI (ctrl-click Scheme→Java) | done |
| **M5** | Completion: in-file symbols → Java members → live Kawa reflection | todo |

## Layout

```
src/main/grammars/Kawa.flex   JFlex lexer       -> generates KawaLexer.java
src/main/grammars/Kawa.bnf    Grammar-Kit BNF   -> generates parser + PSI + KawaTypes
src/main/gen/                 GENERATED (gitignored) — run codegen before compiling
src/main/kotlin/...           Language, file type, parser definition, highlighter, brace matcher
src/main/resources/META-INF/plugin.xml   extension-point registrations
```

## Build & run

Requires JDK 17 or 21 (set one as the Gradle JVM). The build downloads the
target IDE the first time, so the initial run is slow.

```bash
./gradlew generateLexer generateParser   # codegen into src/main/gen (also runs automatically)
./gradlew buildPlugin                     # produces build/distributions/KawaIDEA-*.zip
./gradlew runIde                          # launches a sandbox IDE with the plugin loaded
```

In IntelliJ: open this folder as a Gradle project, then use the **Run Plugin**
(`runIde`) configuration. Open a `.scm` file — you should get highlighting,
brace matching, and (View → Tool Windows → ... / PSI Viewer) a real PSI tree.

### Notes / things to confirm
- `build.gradle.kts` targets IntelliJ IDEA Community `2024.3`. Bump it to match
  the IDE you run, and adjust `sinceBuild` accordingly.
- The plugin claims `.scm`. **Disable your existing Scheme plugin** to avoid a
  file-type conflict.
# KawaIDEA
