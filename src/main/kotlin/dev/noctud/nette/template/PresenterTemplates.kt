package dev.noctud.nette.template

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.PhpClass
import dev.noctud.nette.ext.isPresenter
import java.util.Locale.getDefault

/**
 * Finds the templates of a presenter's views. First where Nette looks for them, then by the rules
 * the Latte plugin uses to tell which presenter a template belongs to, so apps that set the template
 * file themselves get the same answer in both directions.
 */
object PresenterTemplates {

    /** What separates the presenter from the view in a template's name, `Product.edit.latte`. */
    private val NAME_SEPARATORS = listOf(".", "-", "_")

    fun find(presenter: PhpClass, view: String): List<VirtualFile> {
        val presenterDir = presenter.containingFile?.virtualFile?.parent ?: return emptyList()
        val name = presenter.name.removeSuffix("Presenter")

        findByNette(presenterDir, name, view)?.let { return listOf(it) }
        return findByName(presenter, presenterDir, name, view)
    }

    /**
     * The template Nette renders, found the way Presenter::formatTemplateFiles() does:
     * templates/Product/edit.latte or templates/Product.edit.latte in the presenter's directory or
     * the one above it, or edit.latte right next to the presenter when neither has a templates directory.
     */
    private fun findByNette(presenterDir: VirtualFile, name: String, view: String): VirtualFile? {
        val templatesDir = templatesDirIn(presenterDir)
            ?: presenterDir.parent?.let(::templatesDirIn)
            ?: return presenterDir.findChild("$view.latte")

        return templatesDir.findFileByRelativePath("$name/$view.latte")
            ?: templatesDir.findChild("$name.$view.latte")
    }

    private fun templatesDirIn(dir: VirtualFile): VirtualFile? {
        return dir.findChild("templates")?.takeIf { it.isDirectory }
    }

    /**
     * Templates named after the presenter and the view anywhere in the project: Product/edit.latte,
     * Product.edit.latte, Product-edit.latte, Product_edit.latte, and Product.latte for the default
     * view. A template is left to another presenter of the same name when that one is nearer to it,
     * and of the rest the nearest win.
     */
    private fun findByName(presenter: PhpClass, presenterDir: VirtualFile, name: String, view: String): List<VirtualFile> {
        val fileNames = mutableListOf("$view.latte")
        NAME_SEPARATORS.mapTo(fileNames) { "$name$it$view.latte" }
        if (view == "default") {
            fileNames.add("$name.latte")
        }

        val scope = GlobalSearchScope.projectScope(presenter.project)
        val templates = fileNames
            .flatMap { FilenameIndex.getVirtualFilesByName(it, false, scope) }
            .distinct()
            .filter { isTemplateOf(it, name, view) }
        if (templates.isEmpty()) return emptyList()

        val rivalDirs = PhpIndex.getInstance(presenter.project).getClassesByName(presenter.name)
            .filter { it != presenter && !it.isAbstract && it.isPresenter() }
            .mapNotNull { it.containingFile?.virtualFile?.parent }

        val distances = templates
            .filter { template -> rivalDirs.none { distance(template, it) < distance(template, presenterDir) } }
            .associateWith { distance(it, presenterDir) }

        val nearest = distances.values.minOrNull() ?: return emptyList()
        return distances.filterValues { it == nearest }.keys.toList()
    }

    /** Whether the Latte plugin takes the template for the view of the presenter. */
    private fun isTemplateOf(template: VirtualFile, name: String, view: String): Boolean {
        val fileName = template.nameWithoutExtension
        if (template.parent?.name?.capitalized() == name && fileName.decapitalized() == view) {
            return true
        }

        if (fileName.capitalized() == name && view == "default") {
            return true
        }

        return NAME_SEPARATORS.any { separator ->
            val parts = fileName.split(separator)
            parts.size > 1 && parts[0].capitalized() == name && parts[1] == view
        }
    }

    /** How far apart the template and the directory are, counted in characters as the Latte plugin does. */
    private fun distance(template: VirtualFile, dir: VirtualFile): Int {
        val templatePath = template.parent?.path ?: return Int.MAX_VALUE
        val common = templatePath.commonPrefixWith(dir.path).length
        return templatePath.length + dir.path.length - 2 * common
    }

    private fun String.capitalized(): String = replaceFirstChar { it.titlecase(getDefault()) }

    private fun String.decapitalized(): String = replaceFirstChar { it.lowercase(getDefault()) }
}
