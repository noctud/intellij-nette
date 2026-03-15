package dev.noctud.nette.ext

import com.jetbrains.php.lang.psi.elements.*

fun PhpClass.isComponent(): Boolean {
    return isInstanceOf("\\Nette\\Application\\UI\\Component")
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
