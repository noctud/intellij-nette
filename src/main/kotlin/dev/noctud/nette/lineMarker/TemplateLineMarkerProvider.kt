package dev.noctud.nette.lineMarker

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.jetbrains.php.lang.psi.elements.Method
import dev.noctud.nette.ext.*
import dev.noctud.nette.icons.NetteIcons
import dev.noctud.nette.template.PresenterTemplates
import javax.swing.Icon

/**
 * Puts a template icon next to presenter actions that have a template. The icon goes on the render
 * method of the view, or on the action method when the view has no render method.
 */
class TemplateLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun getName(): String = "Nette template"

    override fun getIcon(): Icon = NetteIcons.TEMPLATE

    override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
        val method = element.parent as? Method ?: return
        if (method.nameIdentifier != element) return
        if (!method.isRender() && !method.isAction()) return

        val presenter = method.containingClass ?: return
        if (presenter.isAbstract || !presenter.isPresenter()) return

        if (method.isAction() && presenter.findMethodByName("render" + method.name.removePrefix("action")) != null) return

        val psiManager = PsiManager.getInstance(element.project)
        val templates = PresenterTemplates.find(presenter, method.asActionName()).mapNotNull { psiManager.findFile(it) }
        if (templates.isEmpty()) return

        val tooltip = templates.singleOrNull()?.let { "Navigate to template ${it.parent?.name}/${it.name}" }
            ?: "Navigate to templates"

        val marker = NavigationGutterIconBuilder.create(NetteIcons.TEMPLATE)
            .setTargets(templates)
            .setTooltipText(tooltip)
            .createLineMarkerInfo(element)

        result.add(marker)
    }
}
