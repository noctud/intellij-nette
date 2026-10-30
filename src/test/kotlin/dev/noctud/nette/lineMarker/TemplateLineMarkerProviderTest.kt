package dev.noctud.nette.lineMarker

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.psi.PsiFile
import dev.noctud.nette.BaseNetteTestCase
import dev.noctud.nette.icons.NetteIcons

class TemplateLineMarkerProviderTest : BaseNetteTestCase() {

    /** Template markers in the presenter, as method name to the paths of the templates it leads to. */
    private fun templateMarkers(presenterPath: String, code: String): Map<String, String> {
        val presenter = myFixture.addFileToProject(presenterPath, code)
        myFixture.configureFromExistingVirtualFile(presenter.virtualFile)

        return myFixture.findAllGutters()
            .filterIsInstance<LineMarkerInfo.LineMarkerGutterIconRenderer<*>>()
            .filter { it.icon == NetteIcons.TEMPLATE }
            .associate { renderer ->
                val info = renderer.lineMarkerInfo as RelatedItemLineMarkerInfo<*>
                val targets = info.createGotoRelatedItems().map { (it.element as PsiFile).virtualFile.path.substringAfter("/src/") }
                info.element!!.text to targets.sorted().joinToString(", ")
            }
    }

    private fun presenter(vararg methods: String): String {
        return """
            <?php
            namespace App\UI\Product;
            use Nette\Application\UI\Presenter;
            class ProductPresenter extends Presenter {
                ${methods.joinToString("\n                ") { "public function $it(): void {}" }}
            }
        """.trimIndent()
    }

    fun testTemplateNextToPresenter() {
        myFixture.addFileToProject("app/UI/Product/edit.latte", "")

        val markers = templateMarkers("app/UI/Product/ProductPresenter.php", presenter("renderEdit"))

        assertEquals(mapOf("renderEdit" to "app/UI/Product/edit.latte"), markers)
    }

    fun testTemplateInTemplatesDirectory() {
        myFixture.addFileToProject("app/Presenters/templates/Product/edit.latte", "")

        val markers = templateMarkers("app/Presenters/ProductPresenter.php", presenter("renderEdit"))

        assertEquals(mapOf("renderEdit" to "app/Presenters/templates/Product/edit.latte"), markers)
    }

    fun testTemplateInTemplatesDirectoryOneLevelUp() {
        myFixture.addFileToProject("app/Admin/templates/Product/edit.latte", "")

        val markers = templateMarkers("app/Admin/Presenters/ProductPresenter.php", presenter("renderEdit"))

        assertEquals(mapOf("renderEdit" to "app/Admin/templates/Product/edit.latte"), markers)
    }

    fun testFlatTemplateInTemplatesDirectory() {
        myFixture.addFileToProject("app/Presenters/templates/Product.edit.latte", "")

        val markers = templateMarkers("app/Presenters/ProductPresenter.php", presenter("renderEdit"))

        assertEquals(mapOf("renderEdit" to "app/Presenters/templates/Product.edit.latte"), markers)
    }

    fun testDirectoryTemplateWinsOverFlatTemplate() {
        myFixture.addFileToProject("app/Presenters/templates/Product/edit.latte", "")
        myFixture.addFileToProject("app/Presenters/templates/Product.edit.latte", "")

        val markers = templateMarkers("app/Presenters/ProductPresenter.php", presenter("renderEdit"))

        assertEquals(mapOf("renderEdit" to "app/Presenters/templates/Product/edit.latte"), markers)
    }

    fun testTemplatesDirectoryDisablesTemplateNextToPresenter() {
        myFixture.addFileToProject("app/Presenters/templates/Home/default.latte", "")
        myFixture.addFileToProject("app/Presenters/edit.latte", "")

        val markers = templateMarkers("app/Presenters/ProductPresenter.php", presenter("renderEdit"))

        assertEmpty(markers.keys)
    }

    fun testTemplateInDirectoryNamedAfterPresenter() {
        myFixture.addFileToProject("app/Module/Admin/@Templates/Product/edit.latte", "")

        val markers = templateMarkers("app/Module/Admin/Product/ProductPresenter.php", presenter("actionEdit"))

        assertEquals(mapOf("actionEdit" to "app/Module/Admin/@Templates/Product/edit.latte"), markers)
    }

    fun testFlatTemplateNamedAfterPresenter() {
        myFixture.addFileToProject("app/Module/Admin/@Templates/Product.edit.latte", "")

        val markers = templateMarkers("app/Module/Admin/Product/ProductPresenter.php", presenter("actionEdit"))

        assertEquals(mapOf("actionEdit" to "app/Module/Admin/@Templates/Product.edit.latte"), markers)
    }

    fun testClosestTemplateNamedAfterPresenterWins() {
        myFixture.addFileToProject("app/Module/Admin/@Templates/Product/edit.latte", "")
        myFixture.addFileToProject("app/Module/Front/@Templates/Product/edit.latte", "")

        val markers = templateMarkers("app/Module/Admin/Product/ProductPresenter.php", presenter("actionEdit"))

        assertEquals(mapOf("actionEdit" to "app/Module/Admin/@Templates/Product/edit.latte"), markers)
    }

    fun testEquallyCloseTemplatesNamedAfterPresenterAreAllOffered() {
        myFixture.addFileToProject("app/Module/Admin/@Pages/Product/edit.latte", "")
        myFixture.addFileToProject("app/Module/Admin/@Views/Product/edit.latte", "")

        val markers = templateMarkers("app/Module/Admin/Product/ProductPresenter.php", presenter("actionEdit"))

        assertEquals(
            mapOf("actionEdit" to "app/Module/Admin/@Pages/Product/edit.latte, app/Module/Admin/@Views/Product/edit.latte"),
            markers
        )
    }

    fun testTemplateNamedAfterPresenterWhenTemplatesDirectoryHasNone() {
        myFixture.addFileToProject("app/Presenters/templates/Home/default.latte", "")
        myFixture.addFileToProject("app/Other/Product/edit.latte", "")

        val markers = templateMarkers("app/Presenters/ProductPresenter.php", presenter("renderEdit"))

        assertEquals(mapOf("renderEdit" to "app/Other/Product/edit.latte"), markers)
    }

    fun testTemplateNamedWithDashOrUnderscore() {
        myFixture.addFileToProject("app/Module/Admin/@Templates/Product-edit.latte", "")
        myFixture.addFileToProject("app/Module/Admin/@Templates/product_detail.latte", "")

        val markers = templateMarkers("app/Module/Admin/Product/ProductPresenter.php", presenter("renderEdit", "renderDetail"))

        assertEquals(
            mapOf(
                "renderEdit" to "app/Module/Admin/@Templates/Product-edit.latte",
                "renderDetail" to "app/Module/Admin/@Templates/product_detail.latte",
            ),
            markers
        )
    }

    fun testCapitalizedTemplateInDirectoryNamedAfterPresenter() {
        myFixture.addFileToProject("app/Module/Admin/@Templates/Product/Edit.latte", "")

        val markers = templateMarkers("app/Module/Admin/Product/ProductPresenter.php", presenter("renderEdit"))

        assertEquals(mapOf("renderEdit" to "app/Module/Admin/@Templates/Product/Edit.latte"), markers)
    }

    fun testTemplateNamedAfterPresenterIsDefaultView() {
        myFixture.addFileToProject("app/Module/Admin/@Templates/Product.latte", "")

        val markers = templateMarkers("app/Module/Admin/Product/ProductPresenter.php", presenter("renderDefault", "renderEdit"))

        assertEquals(mapOf("renderDefault" to "app/Module/Admin/@Templates/Product.latte"), markers)
    }

    fun testTemplateOfNearerPresenterOfSameNameIsLeftToIt() {
        myFixture.addFileToProject("app/Module/Front/@Templates/Product/edit.latte", "")
        myFixture.addFileToProject("app/Module/Front/Product/ProductPresenter.php", """
            <?php
            namespace App\Module\Front\Product;
            use Nette\Application\UI\Presenter;
            class ProductPresenter extends Presenter {
                public function renderEdit(): void {}
            }
        """.trimIndent())

        val markers = templateMarkers("app/Module/Admin/Product/ProductPresenter.php", presenter("renderEdit"))

        assertEmpty(markers.keys)
    }

    fun testMarkerOnRenderWhenViewHasBoth() {
        myFixture.addFileToProject("app/UI/Product/edit.latte", "")

        val markers = templateMarkers("app/UI/Product/ProductPresenter.php", presenter("actionEdit", "renderEdit"))

        assertEquals(setOf("renderEdit"), markers.keys)
    }

    fun testMarkerOnActionWithoutRender() {
        myFixture.addFileToProject("app/UI/Product/edit.latte", "")

        val markers = templateMarkers("app/UI/Product/ProductPresenter.php", presenter("actionEdit"))

        assertEquals(setOf("actionEdit"), markers.keys)
    }

    fun testCamelCaseViewName() {
        myFixture.addFileToProject("app/UI/Product/bulkEdit.latte", "")

        val markers = templateMarkers("app/UI/Product/ProductPresenter.php", presenter("renderBulkEdit"))

        assertEquals(setOf("renderBulkEdit"), markers.keys)
    }

    fun testNoMarkerWithoutTemplate() {
        myFixture.addFileToProject("app/UI/Product/default.latte", "")

        val markers = templateMarkers("app/UI/Product/ProductPresenter.php", presenter("renderEdit", "handleDelete", "startup"))

        assertEmpty(markers.keys)
    }

    fun testNoMarkerInAbstractPresenter() {
        myFixture.addFileToProject("app/UI/Base/default.latte", "")

        val markers = templateMarkers("app/UI/Base/BasePresenter.php", """
            <?php
            use Nette\Application\UI\Presenter;
            abstract class BasePresenter extends Presenter {
                public function renderDefault(): void {}
            }
        """.trimIndent())

        assertEmpty(markers.keys)
    }

    fun testNoMarkerOutsidePresenter() {
        myFixture.addFileToProject("app/UI/Menu/default.latte", "")

        val markers = templateMarkers("app/UI/Menu/MenuControl.php", """
            <?php
            use Nette\Application\UI\Control;
            class MenuControl extends Control {
                public function renderDefault(): void {}
            }
        """.trimIndent())

        assertEmpty(markers.keys)
    }
}
