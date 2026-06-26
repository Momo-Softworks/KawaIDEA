package com.momosoftworks.kawaidea

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object KawaFileType : LanguageFileType(KawaLanguage) {
    override fun getName(): String = "Kawa Scheme"
    override fun getDescription(): String = "Kawa Scheme file"
    override fun getDefaultExtension(): String = "scm"
    override fun getIcon(): Icon = KawaIcons.FILE
}
