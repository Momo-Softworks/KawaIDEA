package com.momosoftworks.kawaidea

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.momosoftworks.kawaidea.psi.KawaFile
import com.momosoftworks.kawaidea.psi.KawaForm
import com.momosoftworks.kawaidea.psi.KawaList

/**
 * Project-level cache of top-level Scheme symbols defined across
 * all Kawa files in the project.  Invalidated on file changes
 * (via message bus) and lazily rebuilt on first access.
 */
@Service(Service.Level.PROJECT)
class KawaProjectCache(private val project: Project) : Disposable {

    @Volatile
    private var cached: Set<String>? = null

    init {
        // Invalidate the cache whenever a Kawa file is created, changed, or deleted.
        project.messageBus.connect(this).subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: MutableList<out VFileEvent>) {
                    if (events.any { e -> e.file?.fileType is KawaFileType }) {
                        invalidate()
                    }
                }
            },
        )

        // Also invalidate for unsaved editor edits. This keeps completion aware of
        // source definitions the user just typed, before the file is compiled or saved.
        com.intellij.openapi.editor.EditorFactory.getInstance().eventMulticaster.addDocumentListener(
            object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    val virtualFile = FileDocumentManager.getInstance().getFile(event.document)
                    if (virtualFile?.fileType is KawaFileType) invalidate()
                }
            },
            this,
        )
    }

    override fun dispose() {
        cached = null
    }

    /** Return all top-level symbol names defined in Kawa files in this project. */
    fun allDefinedSymbols(): Set<String> {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val names = mutableSetOf<String>()

            // Only bother if there are Kawa files in the project.
            val scope = GlobalSearchScope.projectScope(project)
            val kawaFiles = FileTypeIndex.getFiles(KawaFileType, scope)

            if (kawaFiles.isEmpty()) {
                cached = emptySet()
                return cached!!
            }

            for (vf in kawaFiles) {
                val psiFile = PsiManager.getInstance(project).findFile(vf) as? KawaFile
                    ?: continue
                val forms: Array<KawaForm> = PsiTreeUtil.getChildrenOfType(
                    psiFile, KawaForm::class.java
                ) ?: continue
                for (form in forms) {
                    val name = extractDefinedSymbol(form)
                    if (name != null) names.add(name)
                }
            }
            cached = names
            return names!!
        }
    }

    /** Force a rescan on next access. */
    fun invalidate() {
        synchronized(this) { cached = null }
    }

    companion object {
        fun getInstance(project: Project): KawaProjectCache =
            project.getService(KawaProjectCache::class.java)

        /** Extract the name defined by a top-level form, if it's a defining form. */
        fun extractDefinedSymbol(form: KawaForm): String? {
            val list = form.list ?: return null
            val forms = list.formList
            val head = forms.firstOrNull()?.atom?.firstChild?.text ?: return null
            if (head !in KawaForms.DEFINING_FORMS) return null
            val second = forms.getOrNull(1) ?: return null

            return when (head) {
                "define" -> {
                    // (define name ...) or (define (name args) ...)
                    second.atom?.firstChild?.text
                        ?: second.list?.formList?.firstOrNull()?.atom?.firstChild?.text
                }
                "define-simple-class", "define-class", "define-private" -> {
                    second.atom?.firstChild?.text
                }
                "define-mod" -> {
                    // (define-mod "id" ...)
                    second.atom?.firstChild?.text
                }
                "define-library" -> {
                    second.list?.formList?.firstOrNull()?.text
                }
                "module-name" -> {
                    second.atom?.firstChild?.text
                }
                else -> second.atom?.firstChild?.text
            }
        }
    }
}
