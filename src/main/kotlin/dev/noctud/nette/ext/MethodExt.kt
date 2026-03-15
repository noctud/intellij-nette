package dev.noctud.nette.ext

import com.jetbrains.php.lang.psi.elements.Method
import java.util.Locale.getDefault

fun Method.asControlName(): String {
    if (!isControl()) {
        throw IllegalStateException("Method is not a control")
    }

    return name.removePrefix("createComponent").replaceFirstChar { it.lowercase(getDefault()) }
}

fun Method.isAnyPresenterMethod(): Boolean {
    return isControl() || isAction() || isRender() || isSignal() || isStartup() || isBeforeRender() || isAfterRender() || isShutdown()
}

fun Method.isControl(): Boolean {
    return name.startsWith("createComponent") && name != "createComponent"
}

fun Method.isAction(): Boolean {
    return name.startsWith("action") && name != "action"
}

fun Method.isRender(): Boolean {
    return name.startsWith("render") && name != "render"
}

fun Method.isSignal(): Boolean {
    return name.startsWith("handle") && name != "handle"
}

fun Method.isStartup(): Boolean {
    return name == "startup"
}

fun Method.isBeforeRender(): Boolean {
    return name == "beforeRender"
}

fun Method.isAfterRender(): Boolean {
    return name == "afterRender"
}

fun Method.isShutdown(): Boolean {
    return name == "shutdown"
}
