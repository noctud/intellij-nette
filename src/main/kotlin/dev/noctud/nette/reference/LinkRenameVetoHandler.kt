package dev.noctud.nette.reference

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.RenameHandler
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import dev.noctud.nette.completion.LinkCompletionContributor
import dev.noctud.nette.ext.*

class LinkRenameVetoHandler : RenameHandler {

    override fun isAvailableOnDataContext(dataContext: DataContext): Boolean {
        val editor = CommonDataKeys.EDITOR.getData(dataContext) ?: return false
        val file = CommonDataKeys.PSI_FILE.getData(dataContext) ?: return false
        return isInsideLinkString(file, editor.caretModel.offset)
    }

    override fun isRenaming(dataContext: DataContext): Boolean = isAvailableOnDataContext(dataContext)

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext?) {
        // Intentionally empty — block rename from link strings
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        // Intentionally empty
    }

    private fun isInsideLinkString(file: PsiFile, offset: Int): Boolean {
        val element = file.findElementAt(offset) ?: return false
        val stringLiteral = PsiTreeUtil.getParentOfType(element, StringLiteralExpression::class.java) ?: return false
        val methodRef = stringLiteral.parent?.parent as? MethodReference ?: return false
        val methodName = methodRef.name ?: return false
        if (methodName !in LinkCompletionContributor.LINK_METHODS) return false
        val params = methodRef.parameters
        if (params.isEmpty() || params[0] != stringLiteral) return false
        val callerClasses = methodRef.classReference?.resolvePhpClasses(true) ?: return false
        return callerClasses.any { it.isNetteLinkSource() }
    }
}
