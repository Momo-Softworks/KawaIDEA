package com.momosoftworks.kawaidea

import com.intellij.formatting.FormattingContext
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.formatting.FormattingModelProvider
import com.intellij.formatting.Indent

class KawaFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val root = KawaBlock(formattingContext.node, null, null, Indent.getNoneIndent())
        return FormattingModelProvider.createFormattingModelForPsiFile(
            formattingContext.containingFile,
            root,
            formattingContext.codeStyleSettings,
        )
    }
}
