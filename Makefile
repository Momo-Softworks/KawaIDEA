# KawaIDEA build targets.
#
# Run inside the project Guix shell (guix.scm) so you never manage JAVA_HOME:
#
#   guix shell -- make          # compile
#   guix shell -- make test     # run tests
#   guix shell -- make plugin   # build distributable zip
#   guix shell -- make run      # sandbox IDE with the plugin
#   guix shell -- make gen      # regenerate lexer + parser from grammars
#   guix shell -- make clean    # wipe build outputs
#
# Or enter the shell once and just type `make`:
#   guix shell
#   make build

# Gradle's jvmToolchain(21) uses toolchain detection which requires JAVA_HOME.
# `guix shell` puts java on PATH but does not set JAVA_HOME, so we derive it.
JAVA_HOME := $(shell java -XshowSettings:all -version 2>&1 | awk '/java\.home/{print $$3}')
export JAVA_HOME

GRADLE = ./gradlew

.PHONY: build test plugin run clean gen

## Default: compile everything.
build:
	$(GRADLE) compileKotlin compileJava

## Run the test suite.
test:
	$(GRADLE) test

## Build the distributable plugin zip.
plugin:
	$(GRADLE) buildPlugin

## Launch a sandbox IntelliJ with the plugin hot-loaded.
run:
	$(GRADLE) runIde

## Regenerate lexer + parser from the .flex / .bnf grammars.
gen:
	$(GRADLE) generateLexer generateParser

## Wipe all Gradle build outputs.
clean:
	$(GRADLE) clean
