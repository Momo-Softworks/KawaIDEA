package com.momosoftworks.kawaidea

import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.momosoftworks.kawaidea.psi.KawaForm
import javax.swing.Icon

/**
 * Structure view for Kawa files: shows top-level defining forms.
 */
class KawaStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder =
        object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel =
                KawaStructureViewModel(psiFile)
        }
}

private class KawaStructureViewModel(file: PsiFile) :
    com.intellij.ide.structureView.StructureViewModelBase(
        file,
        KawaFileRootElement(file),
    ) {

    override fun getSorters(): Array<com.intellij.ide.util.treeView.smartTree.Sorter> = EMPTY_SORTERS
    override fun getFilters(): Array<com.intellij.ide.util.treeView.smartTree.Filter> = EMPTY_FILTERS

    companion object {
        private val EMPTY_SORTERS = emptyArray<com.intellij.ide.util.treeView.smartTree.Sorter>()
        private val EMPTY_FILTERS = emptyArray<com.intellij.ide.util.treeView.smartTree.Filter>()
    }
}

private class KawaFileRootElement(
    private val file: PsiFile,
) : StructureViewTreeElement {

    override fun getValue(): Any = file

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String = file.name ?: "Kawa File"
        override fun getIcon(unused: Boolean): Icon? = null
        override fun getLocationString(): String? = null
    }

    override fun getChildren(): Array<TreeElement> {
        val forms: Array<KawaForm> = PsiTreeUtil.getChildrenOfType(
            file, KawaForm::class.java
        ) ?: emptyArray()
        return forms
            .filter { it.isDefiningForm() }
            .map { KawaFormTreeElement(it) }
            .toTypedArray()
    }
}

private class KawaFormTreeElement(
    private val form: KawaForm,
) : StructureViewTreeElement {

    override fun getValue(): Any = form

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String = label()
        override fun getIcon(unused: Boolean): Icon? = null
        override fun getLocationString(): String? = null
    }

    override fun getChildren(): Array<TreeElement> = emptyArray()

    private fun label(): String {
        val list = form.list ?: return form.text.take(40)
        val forms = list.formList
        val head = forms.firstOrNull()?.atom?.firstChild?.text ?: return "(...)"
        val name = extractName(head, forms)
        return if (name != null) "($head $name)" else "($head ...)"
    }

    private fun extractName(head: String, forms: List<KawaForm>): String? {
        val second = forms.getOrNull(1) ?: return null
        return when (head) {
            "define-mod" -> {
                forms.firstOrNull { f ->
                    f.atom?.firstChild?.text == "name:"
                }?.let { idx ->
                    val nameIdx = forms.indexOf(idx) + 1
                    forms.getOrNull(nameIdx)?.atom?.firstChild?.text
                } ?: second.atom?.firstChild?.text
            }
            "define-library" -> {
                second.list?.formList?.joinToString(" ") { it.text } ?: second.text
            }
            "module-name" -> second.atom?.firstChild?.text
            else -> {
                second.atom?.firstChild?.text
                    ?: second.list?.formList?.firstOrNull()?.atom?.firstChild?.text
            }
        }
    }
}

private fun KawaForm.isDefiningForm(): Boolean {
    val head = this.list?.formList?.firstOrNull()?.atom?.firstChild?.text ?: return false
    return head in KawaForms.DEFINING_FORMS
}
