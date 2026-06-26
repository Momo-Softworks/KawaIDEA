package com.momosoftworks.kawaidea

import com.intellij.application.options.IndentOptionsEditor
import com.intellij.application.options.SmartIndentOptionsEditor
import com.intellij.lang.Language
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider

/**
 * Defaults Kawa indentation to 2 spaces (the Lisp convention) and exposes it
 * under Settings | Editor | Code Style | Kawa, so users can change it.
 */
class KawaCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
    override fun getLanguage(): Language = KawaLanguage

    override fun getIndentOptionsEditor(): IndentOptionsEditor = SmartIndentOptionsEditor()

    override fun customizeDefaults(
        commonSettings: CommonCodeStyleSettings,
        indentOptions: CommonCodeStyleSettings.IndentOptions,
    ) {
        indentOptions.INDENT_SIZE = 2
        indentOptions.CONTINUATION_INDENT_SIZE = 2
        indentOptions.TAB_SIZE = 2
        indentOptions.USE_TAB_CHARACTER = false
    }

    override fun getCodeSample(settingsType: SettingsType): String = CODE_SAMPLE

    companion object {
        private val CODE_SAMPLE =
            """
            (define (square x)
              (* x x))

            (define-mod "patina"
              name: "Patina"
              version: "0.1.0")
            """.trimIndent()
    }
}
