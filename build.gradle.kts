import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.10"
    // IntelliJ Platform Gradle Plugin 2.12+ ships the grammarkit module in-tree,
    // so the grammarkit plugin id needs no separate version.
    id("org.jetbrains.intellij.platform") version "2.12.0"
    id("org.jetbrains.intellij.platform.grammarkit") version "2.12.0"
}

group = "com.momosoftworks"
version = "0.5.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Target a Java-bundling IDE so we can resolve FQNs into Java PSI in M4.
        // Bump this to match the IDE you actually run.
        // Build against the exact IDE installed on this machine (2026.1.3 / 261.25134.95)
        // so behavior matches the runtime precisely.
        local("/opt/intellij-idea-community")
        bundledPlugin("com.intellij.java")
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(21)
}

// The lexer/parser/PSI are generated into src/main/gen; make it a source root.
sourceSets {
    main {
        java {
            srcDirs("src/main/gen")
        }
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "261"
        }
    }
}

tasks {
    generateLexer {
        sourceFile = file("src/main/grammars/Kawa.flex")
        targetOutputDir = file("src/main/gen/com/momosoftworks/kawaidea/lexer")
        purgeOldFiles = true
    }

    generateParser {
        sourceFile = file("src/main/grammars/Kawa.bnf")
        targetRootOutputDir = file("src/main/gen")
        pathToParser = "com/momosoftworks/kawaidea/parser/KawaParser.java"
        pathToPsiRoot = "com/momosoftworks/kawaidea/psi"
        purgeOldFiles = true
    }

    // Codegen must run before anything compiles against it.
    compileKotlin {
        dependsOn(generateLexer, generateParser)
    }
    compileJava {
        dependsOn(generateLexer, generateParser)
    }
}
