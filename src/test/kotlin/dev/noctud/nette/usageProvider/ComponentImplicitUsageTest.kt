package dev.noctud.nette.usageProvider

import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import dev.noctud.nette.BaseNetteTestCase

class ComponentImplicitUsageTest : BaseNetteTestCase() {

    private val provider = ComponentImplicitUsageProvider()

    private fun configurePresenter(code: String) {
        myFixture.configureByText("TestPresenter.php", code)
    }

    private fun findClass(name: String): PhpClass? {
        val classes = com.jetbrains.php.PhpIndex.getInstance(project).getClassesByName(name)
        return classes.firstOrNull()
    }

    private fun findMethod(className: String, methodName: String): Method? {
        return findClass(className)?.findMethodByName(methodName)
    }

    fun testPresenterClassIsImplicitlyUsed() {
        configurePresenter("""
            <?php
            use Nette\Application\UI\Presenter;
            class HomepagePresenter extends Presenter {}
        """.trimIndent())
        val cls = findClass("HomepagePresenter")
        assertNotNull(cls)
        assertTrue("Presenter class should be implicitly used", provider.isImplicitUsage(cls!!))
    }

    fun testActionMethodIsImplicitlyUsed() {
        configurePresenter("""
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function actionDefault(): void {}
            }
        """.trimIndent())
        val method = findMethod("TestPresenter", "actionDefault")
        assertNotNull(method)
        assertTrue("action method should be implicitly used", provider.isImplicitUsage(method!!))
    }

    fun testRenderMethodIsImplicitlyUsed() {
        configurePresenter("""
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function renderDefault(): void {}
            }
        """.trimIndent())
        val method = findMethod("TestPresenter", "renderDefault")
        assertNotNull(method)
        assertTrue("render method should be implicitly used", provider.isImplicitUsage(method!!))
    }

    fun testHandleMethodIsImplicitlyUsed() {
        configurePresenter("""
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function handleClick(): void {}
            }
        """.trimIndent())
        val method = findMethod("TestPresenter", "handleClick")
        assertNotNull(method)
        assertTrue("handle method should be implicitly used", provider.isImplicitUsage(method!!))
    }

    fun testCreateComponentIsImplicitlyUsed() {
        configurePresenter("""
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                protected function createComponentGrid(): \Nette\Application\UI\Control {
                    return new \Nette\Application\UI\Control();
                }
            }
        """.trimIndent())
        val method = findMethod("TestPresenter", "createComponentGrid")
        assertNotNull(method)
        assertTrue("createComponent method should be implicitly used", provider.isImplicitUsage(method!!))
    }

    fun testStartupIsImplicitlyUsed() {
        configurePresenter("""
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function startup(): void { parent::startup(); }
            }
        """.trimIndent())
        val method = findMethod("TestPresenter", "startup")
        assertNotNull(method)
        assertTrue("startup method should be implicitly used", provider.isImplicitUsage(method!!))
    }

    fun testRegularMethodNotImplicitlyUsed() {
        configurePresenter("""
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                private function helperMethod(): void {}
            }
        """.trimIndent())
        val method = findMethod("TestPresenter", "helperMethod")
        assertNotNull(method)
        assertFalse("regular method should NOT be implicitly used", provider.isImplicitUsage(method!!))
    }

    fun testNonNetteClassNotImplicitlyUsed() {
        configurePresenter("""
            <?php
            class RegularClass {
                public function actionDefault(): void {}
            }
        """.trimIndent())
        val method = findMethod("RegularClass", "actionDefault")
        assertNotNull(method)
        assertFalse("action method in non-Nette class should NOT be implicitly used", provider.isImplicitUsage(method!!))
    }

    fun testNonNetteClassIsNotImplicitlyUsed() {
        configurePresenter("""
            <?php
            class PlainClass {}
        """.trimIndent())
        val cls = findClass("PlainClass")
        assertNotNull(cls)
        assertFalse("non-Nette class should NOT be implicitly used", provider.isImplicitUsage(cls!!))
    }
}
