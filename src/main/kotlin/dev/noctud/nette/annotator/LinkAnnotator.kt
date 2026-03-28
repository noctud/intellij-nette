package dev.noctud.nette.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import dev.noctud.nette.completion.LinkCompletionContributor
import dev.noctud.nette.ext.*

class LinkAnnotator : Annotator {
    companion object {
        val LINK_DESTINATION = TextAttributesKey.createTextAttributesKey(
            "NETTE_LINK_DESTINATION",
            DefaultLanguageHighlighterColors.NUMBER
        )
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is StringLiteralExpression) return
        if (DumbService.getInstance(element.project).isDumb) return

        val methodRef = element.parent?.parent as? MethodReference ?: return
        val methodName = methodRef.name ?: return
        if (methodName !in LinkCompletionContributor.LINK_METHODS) return

        val params = methodRef.parameters
        if (params.isEmpty() || params[0] != element) return

        val callerClasses = methodRef.classReference?.resolvePhpClasses(true) ?: return
        if (callerClasses.none { it.isNetteLinkSource() }) return

        // Limit highlight to first line only (unclosed strings extend to EOF)
        val text = element.text
        val newlinePos = text.indexOf('\n')
        val range = if (newlinePos >= 0) {
            TextRange(element.textRange.startOffset, element.textRange.startOffset + newlinePos)
        } else {
            element.textRange
        }

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(range)
            .textAttributes(LINK_DESTINATION)
            .create()
    }
}
