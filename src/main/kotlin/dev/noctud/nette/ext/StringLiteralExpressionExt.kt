package dev.noctud.nette.ext

import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

fun StringLiteralExpression.buildLinkReferences(): Array<PsiReference> {
    val rawContent = contents.substringBefore('\n')
    val valueRange = ElementManipulators.getValueTextRange(this)
    val contentStart = valueRange.startOffset

    // Strip "//" prefix (absolute URL signal in Nette links)
    val slashPrefix = if (rawContent.startsWith("//")) 2 else 0
    val content = rawContent.substring(slashPrefix)

    val isSignal = content.endsWith("!")
    val cleaned = content.trimStart(':')
    val leadingColons = content.length - cleaned.length
    val withoutSignal = cleaned.removeSuffix("!")

    val parts = withoutSignal.split(":")
    if (parts.isEmpty()) return PsiReference.EMPTY_ARRAY

    val references = mutableListOf<PsiReference>()
    var offset = contentStart + slashPrefix + leadingColons

    for ((index, part) in parts.withIndex()) {
        if (part.isEmpty()) {
            offset += 1
            continue
        }

        val isLast = index == parts.lastIndex
        val range = if (isLast && isSignal) {
            TextRange(offset, offset + part.length + 1) // include '!'
        } else {
            TextRange(offset, offset + part.length)
        }

        if (isLast) {
            val presenterParts = parts.subList(0, index).filter { it.isNotEmpty() }
            references.add(LinkActionReference(this, range, presenterParts, part, isSignal))
        } else if (part[0].isUpperCase()) {
            val moduleParts = parts.subList(0, index).filter { it.isNotEmpty() }
            // Prefer abstract when NOT the last presenter part (it's a module, not the target)
            val preferAbstract = index < parts.lastIndex - 1
            references.add(LinkPresenterReference(this, range, moduleParts, part, preferAbstract))
        }

        offset += part.length + 1
    }

    return references.toTypedArray()
}

class LinkPresenterReference(
    element: StringLiteralExpression,
    range: TextRange,
    private val moduleParts: List<String>,
    private val presenterName: String,
    private val preferAbstract: Boolean = false
) : PsiReferenceBase<StringLiteralExpression>(element, range, true) {

    override fun resolve(): PsiElement? {
        val presenters = findPresenters(presenterName, moduleParts, element.project)
        return if (preferAbstract) {
            presenters.firstOrNull { it.isAbstract } ?: presenters.firstOrNull()
        } else {
            presenters.firstOrNull()
        }
    }

    override fun handleElementRename(newElementName: String): PsiElement = element
}

class LinkActionReference(
    element: StringLiteralExpression,
    range: TextRange,
    private val presenterParts: List<String>,
    private val actionName: String,
    private val isSignal: Boolean
) : PsiReferenceBase<StringLiteralExpression>(element, range, true) {

    override fun resolve(): PsiElement? {
        if (actionName.isEmpty()) return null

        val presenterClass = if (presenterParts.isNotEmpty()) {
            val name = presenterParts.last()
            val modules = presenterParts.dropLast(1)
            findPresenters(name, modules, element.project).firstOrNull()
        } else {
            val methodRef = element.parent?.parent as? MethodReference
            methodRef?.classReference?.resolvePhpClasses(true)
                ?.firstOrNull { it.isComponent() }
        } ?: return null

        return if (isSignal) {
            presenterClass.findSignalMethod(actionName)
        } else {
            presenterClass.findActionMethod(actionName)
        }
    }

    override fun handleElementRename(newElementName: String): PsiElement = element
}

class ThisLinkReference(
    element: StringLiteralExpression,
    range: TextRange
) : PsiPolyVariantReferenceBase<StringLiteralExpression>(element, range, true) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        // Inside an action/render method — unambiguous, resolve to that method
        val enclosingMethod = PsiTreeUtil.getParentOfType(element, Method::class.java)
        if (enclosingMethod != null && (enclosingMethod.isAction() || enclosingMethod.isRender())) {
            return arrayOf(PsiElementResolveResult(enclosingMethod))
        }

        // Otherwise show all action methods as targets
        val phpClass = PsiTreeUtil.getParentOfType(element, PhpClass::class.java) ?: return ResolveResult.EMPTY_ARRAY
        if (!phpClass.isComponent()) return ResolveResult.EMPTY_ARRAY

        val seen = HashSet<String>()
        return phpClass.getActions()
            .mapNotNull { method ->
                val name = method.asActionName()
                if (seen.add(name)) phpClass.findActionMethod(name) else null
            }
            .map { PsiElementResolveResult(it) }
            .toTypedArray()
    }

    override fun handleElementRename(newElementName: String): PsiElement = element
}
