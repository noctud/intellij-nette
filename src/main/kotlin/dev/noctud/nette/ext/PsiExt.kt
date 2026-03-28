package dev.noctud.nette.ext

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.*

fun PsiElement.resolvePhpClasses(useIndex: Boolean = false): List<PhpClass> {
    // $this -> containing class
    if (this is Variable && name == "this") {
        PsiTreeUtil.getParentOfType(this, PhpClass::class.java)?.let { return listOf(it) }
    }

    // Direct resolves
    if (this is ClassReference) {
        val resolved = resolve()
        if (resolved is PhpClass) return listOf(resolved)
    }

    if (this is NewExpression) {
        val resolved = classReference?.resolve()
        if (resolved is PhpClass) return listOf(resolved)
    }

    if (this is PhpClass) {
        return listOf(this)
    }

    if (!useIndex) {
        return emptyList()
    }

    // Find out the type of variable
    val typed = this as? PhpTypedElement ?: return emptyList()
    val index = PhpIndex.getInstance(project)
    val result = LinkedHashSet<PhpClass>()
    for (type in typed.type.types.filter { it.startsWith("\\") }) {
        result.addAll(index.getClassesByFQN(type))
    }

    return result.toList()
}
