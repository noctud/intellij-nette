package dev.noctud.nette.typeProvider

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4
import dev.noctud.nette.ext.*

/**
 * Types `$this->template` and `$this->getTemplate()` with the template class Nette picks by
 * convention in formatTemplateClass(): ProductEditTemplate in the edit action of ProductPresenter,
 * otherwise ProductTemplate, and MenuTemplate in MenuControl.
 */
class TemplateTypeProvider : PhpTypeProvider4 {
    private val separator = '\u001f'

    override fun getKey(): Char = 'ɴ'

    override fun getType(element: PsiElement): PhpType? {
        val classReference = when (element) {
            is FieldReference -> element.takeIf { it.name == "template" }?.classReference
            is MethodReference -> element.takeIf { it.name == "getTemplate" }?.classReference
            else -> null
        }
        if (classReference !is Variable || classReference.name != "this") return null

        val phpClass = PsiTreeUtil.getParentOfType(element, PhpClass::class.java) ?: return null
        val method = PsiTreeUtil.getParentOfType(element, Method::class.java)?.takeIf { it.containingClass == phpClass }
        val action = when {
            method == null -> ""
            method.isAction() -> method.name.removePrefix("action")
            method.isRender() -> method.name.removePrefix("render")
            else -> ""
        }

        // Whether the class is a presenter or a control needs the index, so complete() tells
        return PhpType().add("#$key${phpClass.fqn}$separator$action")
    }

    override fun complete(expression: String, project: Project): PhpType? {
        if (!expression.startsWith("#$key")) {
            return null
        }

        val parts = expression.substring(2).split(separator)
        if (parts.size != 2) {
            return null
        }

        val (classFqn, action) = parts
        val index = PhpIndex.getInstance(project)
        val phpClass = index.getClassesByFQN(classFqn).firstOrNull() ?: return null

        val candidates = when {
            phpClass.isPresenter() -> {
                val base = classFqn.removeSuffix("Presenter")
                listOfNotNull(if (action.isNotEmpty()) "$base${action}Template" else null, "${base}Template")
            }
            phpClass.isInstanceOf("\\Nette\\Application\\UI\\Control") -> listOf(classFqn.removeSuffix("Control") + "Template")
            else -> return null
        }

        for (candidate in candidates) {
            val template = index.getClassesByFQN(candidate).firstOrNull { it.isInstanceOf("\\Nette\\Application\\UI\\Template") }
            if (template != null) {
                return PhpType().add(template.fqn)
            }
        }

        return null
    }

    override fun getBySignature(
        expression: String,
        set: Set<String>,
        depth: Int,
        project: Project
    ): MutableCollection<out PhpNamedElement> {
        return mutableListOf()
    }
}
