package dev.noctud.nette.reference

import com.intellij.openapi.project.DumbService
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import dev.noctud.nette.ext.*

class LinkReferenceContributor : PsiReferenceContributor() {
    companion object {
        val LINK_METHODS = setOf(
            "link", "redirect", "forward", "isLinkCurrent",
            "canonicalize", "lazyLink", "redirectPermanent"
        )
    }

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression::class.java),
            LinkReferenceProvider()
        )
    }
}

private class LinkReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val stringLiteral = element as? StringLiteralExpression ?: return PsiReference.EMPTY_ARRAY

        if (DumbService.getInstance(stringLiteral.project).isDumb) return PsiReference.EMPTY_ARRAY

        val methodRef = stringLiteral.parent?.parent as? MethodReference ?: return PsiReference.EMPTY_ARRAY
        val methodName = methodRef.name ?: return PsiReference.EMPTY_ARRAY
        if (methodName !in LinkReferenceContributor.LINK_METHODS) return PsiReference.EMPTY_ARRAY

        val params = methodRef.parameters
        if (params.isEmpty() || params[0] != stringLiteral) return PsiReference.EMPTY_ARRAY

        val callerClasses = methodRef.classReference?.resolvePhpClasses(true) ?: return PsiReference.EMPTY_ARRAY
        if (callerClasses.none { it.isNetteLinkSource() }) return PsiReference.EMPTY_ARRAY

        val content = stringLiteral.contents
        if (content.isEmpty()) return PsiReference.EMPTY_ARRAY

        if (content == "this") {
            val valueRange = ElementManipulators.getValueTextRange(stringLiteral)
            return arrayOf(ThisLinkReference(stringLiteral, valueRange))
        }

        return stringLiteral.buildLinkReferences()
    }
}
