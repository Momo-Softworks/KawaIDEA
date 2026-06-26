package com.momosoftworks.kawaidea.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import com.momosoftworks.kawaidea.KawaFileType
import com.momosoftworks.kawaidea.KawaLanguage

class KawaFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, KawaLanguage) {
    override fun getFileType() = KawaFileType
    override fun toString(): String = "Kawa File"
}
