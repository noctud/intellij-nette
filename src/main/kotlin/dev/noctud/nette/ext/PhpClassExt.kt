package dev.noctud.nette.ext

import com.intellij.openapi.project.Project
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.*
import java.util.Locale.getDefault

fun PhpClass.isComponent(): Boolean {
    return isInstanceOf("\\Nette\\Application\\UI\\Component")
}

fun PhpClass.isLinkGenerator(): Boolean {
    return isInstanceOf("\\Nette\\Application\\LinkGenerator")
}

/** Whether this class can generate Nette links (Component, Presenter, or LinkGenerator). */
fun PhpClass.isNetteLinkSource(): Boolean {
    return isComponent() || isLinkGenerator()
}

fun PhpClass.isInstanceOf(fqn: String): Boolean {
    var current: PhpClass? = this

    while (current != null) {
        if (current.fqn == fqn) {
            return true
        }

        if (current.implementedInterfaces.any { it.fqn == fqn }) {
            return true
        }

        current = current.superClass
    }

    return false
}

fun PhpClass.getControls(): List<Method> {
    return methods.filter { it.isControl() }
}

fun PhpClass.isPresenter(): Boolean {
    return isInstanceOf("\\Nette\\Application\\UI\\Presenter")
}

fun PhpClass.getActions(): List<Method> {
    return methods.filter { it.isAction() || it.isRender() }
}

fun PhpClass.getSignals(): List<Method> {
    return methods.filter { it.isSignal() && it.name != "handleInvalidLink" }
}

fun PhpClass.asPresenterName(): String {
    var result = name.removeSuffix("Presenter")
    if (result.startsWith("Abstract")) result = result.removePrefix("Abstract")
    else if (result.startsWith("Base")) result = result.removePrefix("Base")
    return result
}

fun PhpClass.findActionMethod(actionName: String): Method? {
    val capitalized = actionName.replaceFirstChar { it.uppercase(getDefault()) }
    return findMethodByName("action$capitalized")
        ?: findMethodByName("render$capitalized")
}

fun PhpClass.findSignalMethod(signalName: String): Method? {
    val capitalized = signalName.replaceFirstChar { it.uppercase(getDefault()) }
    return findMethodByName("handle$capitalized")
}

fun findPresenters(name: String, modules: List<String>, project: Project): List<PhpClass> {
    return getAllPresenters(project)
        .filter { it.asPresenterName() == name && it.hasModuleAncestors(modules) }
        .sortedBy { it.isAbstract }
}

/**
 * Checks if this presenter's superclass chain contains presenters matching
 * the given module names in order (innermost first when walking up).
 * E.g. for DetailPresenter extends BaseServerPresenter extends BaseWebPresenter,
 * hasModuleAncestors(["Web", "Server"]) returns true.
 */
fun PhpClass.hasModuleAncestors(modules: List<String>): Boolean {
    if (modules.isEmpty()) return true
    var current = superClass
    val remaining = modules.toMutableList()
    while (current != null && remaining.isNotEmpty()) {
        val name = current.asPresenterName()
        if (name.isNotEmpty() && name == remaining.last()) {
            remaining.removeLast()
        }
        current = current.superClass
    }
    return remaining.isEmpty()
}

fun getAllPresenters(project: Project): List<PhpClass> {
    val index = PhpIndex.getInstance(project)
    val base = index.getAnyByFQN("\\Nette\\Application\\UI\\Presenter").firstOrNull() ?: return emptyList()
    return collectSubclasses(base, index)
}

private fun collectSubclasses(phpClass: PhpClass, index: PhpIndex): List<PhpClass> {
    val result = mutableListOf<PhpClass>()
    for (subclass in index.getDirectSubclasses(phpClass.fqn)) {
        result.add(subclass)
        result.addAll(collectSubclasses(subclass, index))
    }
    return result
}
