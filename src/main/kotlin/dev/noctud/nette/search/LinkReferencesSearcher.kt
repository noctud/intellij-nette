package dev.noctud.nette.search

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiReference
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import dev.noctud.nette.ext.*

class LinkReferencesSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {
    override fun processQuery(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>
    ) {
        val element = queryParameters.elementToSearch

        val searchName = when (element) {
            is Method if element.isAction() && element.containingClass?.isComponent() == true -> element.asActionName()
            is Method if element.isRender() && element.containingClass?.isComponent() == true -> element.asActionName()
            is Method if element.isSignal() && element.containingClass?.isComponent() == true -> element.asSignalName()
            is PhpClass if element.isPresenter() -> element.asPresenterName()
            else -> return
        }

        if (searchName.isEmpty()) return

        queryParameters.optimizer.searchWord(
            searchName,
            queryParameters.effectiveSearchScope,
            UsageSearchContext.IN_STRINGS,
            true,
            element
        )
    }
}
