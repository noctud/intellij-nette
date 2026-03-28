package dev.noctud.nette.reference

import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import dev.noctud.nette.BaseNetteTestCase

class LinkReferenceTest : BaseNetteTestCase() {

    private fun resolveAt(code: String): com.intellij.psi.PsiElement? {
        myFixture.configureByText("TestPresenter.php", code)
        return myFixture.file.findReferenceAt(myFixture.caretOffset)?.resolve()
    }

    fun testActionResolvesToMethod() {
        val resolved = resolveAt("""
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function actionDefault(): void {
                    ${'$'}this->link('det<caret>ail');
                }
                public function actionDetail(): void {}
            }
        """.trimIndent())
        assertNotNull("Should resolve", resolved)
        assertInstanceOf(resolved, Method::class.java)
        assertEquals("actionDetail", (resolved as Method).name)
    }

    fun testFallsBackToRenderMethod() {
        val resolved = resolveAt("""
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function actionDefault(): void {
                    ${'$'}this->link('det<caret>ail');
                }
                public function renderDetail(): void {}
            }
        """.trimIndent())
        assertNotNull("Should resolve to render when no action exists", resolved)
        assertEquals("renderDetail", (resolved as Method).name)
    }

    fun testSignalResolvesToHandleMethod() {
        val resolved = resolveAt("""
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function actionDefault(): void {
                    ${'$'}this->link('cli<caret>ck!');
                }
                public function handleClick(): void {}
            }
        """.trimIndent())
        assertNotNull("Should resolve signal", resolved)
        assertEquals("handleClick", (resolved as Method).name)
    }

    fun testPresenterPartResolvesToClass() {
        myFixture.addFileToProject(
            "HomepagePresenter.php", """
            <?php
            use Nette\Application\UI\Presenter;
            class HomepagePresenter extends Presenter {
                public function actionDefault(): void {}
            }
        """.trimIndent()
        )
        val resolved = resolveAt("""
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function actionDefault(): void {
                    ${'$'}this->link('Home<caret>page:default');
                }
            }
        """.trimIndent())
        assertNotNull("Presenter part should resolve", resolved)
        assertInstanceOf(resolved, PhpClass::class.java)
        assertEquals("HomepagePresenter", (resolved as PhpClass).name)
    }

    fun testCrossPresenterActionResolves() {
        myFixture.addFileToProject(
            "HomepagePresenter.php", """
            <?php
            use Nette\Application\UI\Presenter;
            class HomepagePresenter extends Presenter {
                public function actionDetail(): void {}
            }
        """.trimIndent()
        )
        val resolved = resolveAt("""
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function actionDefault(): void {
                    ${'$'}this->redirect('Homepage:det<caret>ail');
                }
            }
        """.trimIndent())
        assertNotNull("Cross-presenter action should resolve", resolved)
        assertEquals("actionDetail", (resolved as Method).name)
    }

    fun testThisResolvesInsideActionMethod() {
        val resolved = resolveAt("""
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function actionDefault(): void {
                    ${'$'}this->link('th<caret>is');
                }
            }
        """.trimIndent())
        assertNotNull("'this' should resolve inside action method", resolved)
        assertEquals("actionDefault", (resolved as Method).name)
    }

    fun testThisResolvesWithSingleAction() {
        val resolved = resolveAt("""
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function actionDefault(): void {}
                public function handleClick(): void {
                    ${'$'}this->link('th<caret>is');
                }
            }
        """.trimIndent())
        assertNotNull("'this' should resolve when only one action exists", resolved)
        assertEquals("actionDefault", (resolved as Method).name)
    }

    fun testThisMultiResolvesWithMultipleActions() {
        myFixture.configureByText("TestPresenter.php", """
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function actionDefault(): void {}
                public function actionDetail(): void {}
                public function handleClick(): void {
                    ${'$'}this->link('th<caret>is');
                }
            }
        """.trimIndent())
        val ref = myFixture.file.findReferenceAt(myFixture.caretOffset)
        assertNotNull("Should have a reference", ref)
        assertInstanceOf(ref, com.intellij.psi.PsiPolyVariantReference::class.java)
        val results = (ref as com.intellij.psi.PsiPolyVariantReference).multiResolve(false)
        assertEquals("Should resolve to both actions", 2, results.size)
    }

    fun testModuleHierarchyResolves() {
        myFixture.addFileToProject(
            "presenters.php", """
            <?php
            use Nette\Application\UI\Presenter;
            abstract class BaseWebPresenter extends Presenter {}
            abstract class BaseServerPresenter extends BaseWebPresenter {}
            class DetailPresenter extends BaseServerPresenter {
                public function actionDefault(): void {}
            }
        """.trimIndent()
        )
        val resolved = resolveAt("""
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function actionDefault(): void {
                    ${'$'}this->link(':Web:Server:Detail:def<caret>ault');
                }
            }
        """.trimIndent())
        assertNotNull("Module hierarchy should resolve", resolved)
        assertEquals("actionDefault", (resolved as Method).name)
    }

    fun testNoReferenceInNonNetteClass() {
        val resolved = resolveAt("""
            <?php
            class Router {
                public function redirect(string ${'$'}url): void {}
                public function doStuff(): void {
                    ${'$'}this->redirect('some<caret>thing');
                }
            }
        """.trimIndent())
        assertNull("Should not resolve in non-Nette class", resolved)
    }
}
