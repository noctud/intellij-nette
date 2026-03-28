package dev.noctud.nette.completion

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbService
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.ElementManipulators
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import dev.noctud.nette.ext.*

class LinkCompletionContributor : CompletionContributor() {
    companion object {
        val LINK_METHODS = setOf(
            "link", "redirect", "forward", "isLinkCurrent",
            "canonicalize", "lazyLink", "redirectPermanent"
        )
    }

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withParent(StringLiteralExpression::class.java),
            LinkCompletionProvider()
        )
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        val element = context.file.findElementAt(context.startOffset) ?: return
        val stringLiteral = element.parent as? StringLiteralExpression ?: return
        if (!stringLiteral.text.contains('\n')) return

        val methodRef = stringLiteral.parent?.parent as? MethodReference ?: return
        if (methodRef.name !in LINK_METHODS) return

        // Unclosed strings extend to the next quote in the file.
        // Limit replacement to the cursor so we don't eat the rest of the code.
        context.replacementOffset = context.caret.offset
    }

    private class LinkCompletionProvider : CompletionProvider<CompletionParameters?>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val position = parameters.originalPosition ?: parameters.position
            val stringLiteral = position.parent as? StringLiteralExpression ?: return

            if (DumbService.getInstance(stringLiteral.project).isDumb) return

            val methodRef = stringLiteral.parent?.parent as? MethodReference ?: return
            val methodName = methodRef.name ?: return
            if (methodName !in LINK_METHODS) return

            val methodParams = methodRef.parameters
            if (methodParams.isEmpty() || methodParams[0] != stringLiteral) return

            val callerClass = methodRef.classReference?.resolvePhpClasses(true)
                ?.firstOrNull { it.isNetteLinkSource() } ?: return

            // Use full string content up to cursor, not just the prefix matcher's prefix
            // (colon acts as a word boundary so the prefix matcher only sees text after the last colon)
            val valueRange = ElementManipulators.getValueTextRange(stringLiteral)
            val contentStartInFile = stringLiteral.textRange.startOffset + valueRange.startOffset
            val cursorInContent = (parameters.offset - contentStartInFile).coerceIn(0, stringLiteral.contents.length)
            val contentUpToCursor = stringLiteral.contents.substring(0, cursorInContent)
                .removePrefix("//")

            val lastColon = contentUpToCursor.lastIndexOf(':')

            if (lastColon >= 0) {
                val presenterPart = contentUpToCursor.substring(0, lastColon).trimStart(':')
                val parts = presenterPart.split(":").filter { it.isNotEmpty() }
                val allPresenters = getAllPresenters(callerClass.project)

                // IntelliJ doesn't treat ':' as a word boundary in strings, so the prefix
                // includes everything (e.g. "Web:DomainList"). Override to just the text after
                // the last colon so "DomainList" matches "DomainList:" lookup strings.
                val afterColon = contentUpToCursor.substring(lastColon + 1)
                val adjustedResult = result.withPrefixMatcher(afterColon)

                if (parts.isNotEmpty()) {
                    val presenterName = parts.last()
                    val moduleParts = parts.dropLast(1)
                    val presenters = findPresenters(presenterName, moduleParts, callerClass.project)

                    for (presenter in presenters) {
                        // Actions only for concrete presenters — abstract ones are modules
                        if (!presenter.isAbstract) {
                            val seenActions = HashSet<String>()
                            for (method in presenter.getActions()) {
                                val name = method.asActionName()
                                if (seenActions.add(name)) {
                                    adjustedResult.addElement(
                                        LookupElementBuilder.create(name)
                                            .withTypeText(presenter.name, true)
                                    )
                                }
                            }
                        }

                        // Child presenters (sub-modules and concrete presenters under this one)
                        addChildPresenterCompletions(presenter, allPresenters, adjustedResult)
                    }
                } else {
                    // Just a leading colon (e.g., ":") — show all presenters as root entry points
                    val seen = HashSet<String>()
                    for (presenter in allPresenters) {
                        val name = presenter.asPresenterName()
                        if (name.isNotEmpty() && seen.add(name)) {
                            adjustedResult.addElement(
                                LookupElementBuilder.create("$name:")
                                    .withPresentableText(name)
                                    .withTypeText(presenter.name, true)
                                    .withLookupString(name.lowercase())
                            .withInsertHandler(presenterInsertHandler)
                            )
                        }
                    }
                }
            } else {
                // No colon — show actions/signals on current class + presenter names
                val seenActions = HashSet<String>()
                for (method in callerClass.getActions()) {
                    val name = method.asActionName()
                    if (seenActions.add(name)) {
                        result.addElement(
                            LookupElementBuilder.create(name)
                                .withTypeText(callerClass.name, true)
                        )
                    }
                }
                result.addElement(
                    LookupElementBuilder.create("this")
                        .withTypeText("current action", true)
                        .bold()
                )

                // Signals only for current presenter (no cross-presenter signals)
                for (method in callerClass.getSignals()) {
                    val name = method.asSignalName()
                    result.addElement(
                        LookupElementBuilder.create("$name!")
                            .withTypeText(callerClass.name, true)
                            .withInsertHandler(signalInsertHandler)
                    )
                }

                val seen = HashSet<String>()
                for (presenter in getAllPresenters(callerClass.project)) {
                    if (presenter.isAbstract) continue
                    val name = presenter.asPresenterName()
                    if (name.isNotEmpty() && seen.add(name)) {
                        result.addElement(
                            LookupElementBuilder.create("$name:")
                                .withPresentableText(name)
                                .withTypeText(presenter.fqn, true)
                                .withLookupString(name.lowercase())
                            .withInsertHandler(presenterInsertHandler)
                        )
                    }
                }
            }
        }

        private fun addChildPresenterCompletions(
            parent: PhpClass,
            allPresenters: List<PhpClass>,
            result: CompletionResultSet
        ) {
            val seen = HashSet<String>()
            for (child in allPresenters) {
                if (!isDirectModuleChild(child, parent)) continue
                val name = child.asPresenterName()
                if (name.isNotEmpty() && seen.add(name)) {
                    result.addElement(
                        LookupElementBuilder.create("$name:")
                            .withPresentableText(name)
                            .withTypeText(child.name, true)
                            .withLookupString(name.lowercase())
                            .withInsertHandler(presenterInsertHandler)
                    )
                }
            }
        }

        /**
         * Checks if [child]'s first named module ancestor is [parent],
         * skipping intermediate base classes with empty presenter names.
         */
        private fun isDirectModuleChild(child: PhpClass, parent: PhpClass): Boolean {
            var current = child.superClass
            while (current != null) {
                if (current.fqn == parent.fqn) return true
                if (current.asPresenterName().isNotEmpty()) return false
                current = current.superClass
            }
            return false
        }

        private val signalInsertHandler = InsertHandler<com.intellij.codeInsight.lookup.LookupElement> { ctx, _ ->
            // Eat trailing '!' if already present to avoid duplication (e.g. 'signal!!')
            val offset = ctx.tailOffset
            val doc = ctx.document
            if (offset < doc.textLength && doc.charsSequence[offset] == '!') {
                doc.deleteString(offset, offset + 1)
            }
        }

        private val presenterInsertHandler = InsertHandler<com.intellij.codeInsight.lookup.LookupElement> { ctx, item ->
            val doc = ctx.document
            val start = ctx.startOffset
            val tail = ctx.tailOffset

            // Force correct casing — IntelliJ may lowercase the insert to match what the user typed
            val correct = item.lookupString
            doc.replaceString(start, tail, correct)
            val newTail = start + correct.length
            ctx.editor.caretModel.moveToOffset(newTail)

            // Eat trailing ':' if already present to avoid duplication (e.g. 'Presenter::')
            if (newTail < doc.textLength && doc.charsSequence[newTail] == ':') {
                doc.deleteString(newTail, newTail + 1)
            }
            AutoPopupController.getInstance(ctx.project).scheduleAutoPopup(ctx.editor)
        }
    }
}
