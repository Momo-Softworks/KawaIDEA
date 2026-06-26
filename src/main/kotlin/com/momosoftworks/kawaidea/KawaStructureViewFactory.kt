package com.momosoftworks.kawaidea

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
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
                KawaStructureViewModel(editor, psiFile)
        }
}

private class KawaStructureViewModel(
    editor: Editor?,
    file: PsiFile,
) : StructureViewModelBase(file, editor, KawaStructureRootElement(file)),
    StructureViewModel.ElementInfoProvider {

    override fun getSorters(): Array<Sorter> = arrayOf(Sorter.ALPHA_SORTER)

    override fun getSuitableClasses(): Array<Class<*>> =
        arrayOf(KawaForm::class.java)

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean = false
}

/**
 * Root element wrapping the Kawa file.
 */
private class KawaStructureRootElement(
    private val file: PsiFile,
) : StructureViewTreeElement, SortableTreeElement {

    override fun getValue(): Any = file

    override fun getAlphaSortKey(): String = file.name ?: ""

    override fun getPresentation(): ItemPresentation = file.presentation
        ?: PresentationData(file.name ?: "Kawa File", file.toString(), null, null)

    override fun getChildren(): Array<TreeElement> {
        val forms: Array<KawaForm> = PsiTreeUtil.getChildrenOfType(
            file, KawaForm::class.java
        ) ?: emptyArray()
        return forms
            .filter { it.isDefiningForm() }
            .map { KawaDefiningFormElement(it) }
            .toTypedArray()
    }

    override fun navigate(requestFocus: Boolean) {
        file.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean = file.canNavigate()

    override fun canNavigateToSource(): Boolean = file.canNavigateToSource()
}

/**
 * A single defining form in the structure view.
 */
private class KawaDefiningFormElement(
    private val form: KawaForm,
) : StructureViewTreeElement, SortableTreeElement {

    override fun getValue(): Any = form

    override fun getAlphaSortKey(): String {
        val list = form.list ?: return ""
        val head = list.formList.firstOrNull()?.atom?.firstChild?.text ?: return ""
        val name = extractName(head, list.formList) ?: return head
        return name
    }

    override fun getPresentation(): ItemPresentation {
        val list = form.list
        if (list == null) return PresentationData(form.text.take(40), null, null, null)
        val forms = list.formList
        val head = forms.firstOrNull()?.atom?.firstChild?.text ?: "(...)"
        val name = extractName(head, forms)
        val text = if (name != null) "($head $name)" else "($head ...)"
        return PresentationData(text, null, null, null)
    }

    override fun getChildren(): Array<TreeElement> = emptyArray()

    override fun navigate(requestFocus: Boolean) {
        form.containingFile?.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean =
        form.containingFile?.canNavigate() ?: false

    override fun canNavigateToSource(): Boolean =
        form.containingFile?.canNavigateToSource() ?: false

    companion object {
        fun extractName(head: String, forms: List<KawaForm>): String? {
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
}

private fun KawaForm.isDefiningForm(): Boolean {
    val head = this.list?.formList?.firstOrNull()?.atom?.firstChild?.text ?: return false
    return head in KawaForms.DEFINING_FORMS
}
