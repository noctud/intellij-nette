package dev.noctud.nette.typeProvider

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4
import dev.noctud.nette.ext.*

class ControlTypeProvider : PhpTypeProvider4 {
    private val separator = '\u001f'

    override fun getKey(): Char = 'N'

    override fun getType(element: PsiElement): PhpType? {
        if (DumbService.getInstance(element.project).isDumb) {
            return null
        }

        var candidate: PsiElement? = null
        var componentName: String? = null

        // Resolve $this['abc']
        if (element is ArrayAccessExpression) {
            val index = element.index ?: return null
            val idxValue = index.value ?: return null
            if (idxValue !is StringLiteralExpression) return null
            candidate = element.value ?: return null
            componentName = idxValue.contents
        }

        // Resolve $this->getComponent('abc')
        if (element is MethodReference) {
            val name = element.name ?: return null
            if (name != "getComponent") return null
            val parameters = element.parameters
            if (parameters.isEmpty()) return null
            val first = parameters[0]
            if (first !is StringLiteralExpression) return null
            candidate = element.classReference ?: return null
            componentName = first.contents
        }

        if (candidate != null) {
            // We cannot use index in resolvePhpClasses because of possible recursion
            for (phpClass in candidate.resolvePhpClasses().filter { it.isComponent() }) {
                return phpClass.getControls().firstOrNull { it.asControlName() == componentName }?.type ?: continue
            }

            // That's why we pass the info to the complete() and do it there
            val type = (candidate as? PhpTypedElement)?.type
            if (type == null || type.isEmpty) return null

            return PhpType().add("#$key$type$separator$componentName")
        }

        return null
    }

    override fun complete(expression: String, project: Project): PhpType? {
        if (!expression.startsWith("#$key")) {
            return null
        }

        val content = expression.substring(2)
        val parts = content.split(separator)
        if (parts.size != 2) {
            return null
        }

        val index = PhpIndex.getInstance(project)
        val classes = index.completeType(project, PhpType().add(parts[0]), null)

        for (parentType in classes.types) {
            for (phpClass in index.getClassesByFQN(parentType).filter { it.isComponent() }) {
                return phpClass.getControls().firstOrNull { it.asControlName() == parts[1] }?.type ?: continue
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
