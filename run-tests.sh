#!/usr/bin/env bash
# Run pure-JUnit KawaIDEA tests that do not need the IntelliJ runtime.
# Bypasses Gradle's test task (which requires the IntelliJ JBR, broken on Guix).
#
# Usage:  ./run-tests.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# -- compile tests via Gradle -------------------------------------------------
echo "=== Compiling ==="
./gradlew --no-daemon --no-configuration-cache compileTestKotlin 2>&1 | tail -3

# -- assemble classpath ------------------------------------------------------
CP="build/classes/kotlin/main:build/classes/kotlin/test"

# Kotlin stdlib (our classes are compiled with Kotlin)
KOTLIN_STDLIB=$(find /home/swilley/.gradle/caches -name "kotlin-stdlib-2*.jar" -not -name "*sources*" 2>/dev/null | head -1)
[ -n "$KOTLIN_STDLIB" ] || KOTLIN_STDLIB=$(find /home/swilley/.gradle/caches -name "kotlin-stdlib-*.jar" -not -name "*sources*" 2>/dev/null | head -1)
[ -n "$KOTLIN_STDLIB" ] || { echo "ERROR: kotlin-stdlib jar not found"; exit 1; }
CP="$CP:$KOTLIN_STDLIB"

# JUnit + Hamcrest from Gradle cache
JUNIT_JAR=$(find /home/swilley/.gradle/caches -name "junit-4*.jar" -not -name "*sources*" 2>/dev/null | head -1)
HAMCREST_JAR=$(find /home/swilley/.gradle/caches -name "hamcrest-core-*.jar" 2>/dev/null | head -1)

[ -n "$JUNIT_JAR" ] || { echo "ERROR: junit jar not found"; exit 1; }
CP="$CP:$JUNIT_JAR"
[ -n "$HAMCREST_JAR" ] && CP="$CP:$HAMCREST_JAR"

# -- run --------------------------------------------------------------------
failures=0

run_test() {
    local cls="$1"
    echo "=== $cls ==="
    if java -cp "$CP" org.junit.runner.JUnitCore "$cls" 2>&1; then
        echo "  PASS"
    else
        echo "  FAIL"
        failures=$((failures + 1))
    fi
    echo ""
}

run_test com.momosoftworks.kawaidea.KawaNamesTest
run_test com.momosoftworks.kawaidea.KawaCompletionLogicTest
run_test com.momosoftworks.kawaidea.KawaFormsTest

echo "=== Done: $failures failure(s) ==="
exit $failures
