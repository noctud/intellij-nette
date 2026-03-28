package dev.noctud.nette.completion

import dev.noctud.nette.BaseNetteTestCase

class LinkCompletionTest : BaseNetteTestCase() {

    private fun addHomepagePresenter() {
        myFixture.addFileToProject(
            "HomepagePresenter.php", """
            <?php
            use Nette\Application\UI\Presenter;
            class HomepagePresenter extends Presenter {
                public function actionDefault(): void {}
                public function actionDetail(int ${'$'}id): void {}
                public function renderDefault(): void {}
                public function handleClick(): void {}
                public function handleSort(): void {}
            }
        """.trimIndent()
        )
    }

    private fun addAdminPresenter() {
        myFixture.addFileToProject(
            "AdminPresenter.php", """
            <?php
            use Nette\Application\UI\Presenter;
            class AdminPresenter extends Presenter {
                public function actionDashboard(): void {}
            }
        """.trimIndent()
        )
    }

    private fun completionAt(code: String): List<String> {
        myFixture.configureByText("TestPresenter.php", code)
        val completions = myFixture.completeBasic()
        return completions?.map { it.lookupString } ?: emptyList()
    }

    fun testActionsOfferedInLink() {
        addHomepagePresenter()
        val items = completionAt("""
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function actionDefault(): void {
                    ${'$'}this->link('<caret>');
                }
            }
        """.trimIndent())
        assertContainsElements(items, "default")
        assertContainsElements(items, "this")
    }

    fun testSignalsOfferedWithBang() {
        val items = completionAt("""
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function handleRefresh(): void {}
                public function actionDefault(): void {
                    ${'$'}this->link('<caret>');
                }
            }
        """.trimIndent())
        assertContainsElements(items, "refresh!")
    }

    fun testPresenterNamesOffered() {
        addHomepagePresenter()
        val items = completionAt("""
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function actionDefault(): void {
                    ${'$'}this->link('<caret>');
                }
            }
        """.trimIndent())
        assertContainsElements(items, "Homepage:")
    }

    fun testActionsAfterPresenterColon() {
        addHomepagePresenter()
        val items = completionAt("""
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function actionDefault(): void {
                    ${'$'}this->link('Homepage:<caret>');
                }
            }
        """.trimIndent())
        assertContainsElements(items, "default", "detail")
    }

    fun testNoSignalsAfterPresenterColon() {
        addHomepagePresenter()
        val items = completionAt("""
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function actionDefault(): void {
                    ${'$'}this->link('Homepage:<caret>');
                }
            }
        """.trimIndent())
        assertDoesntContain(items, "click!", "sort!")
    }

    fun testDeduplatesActionAndRender() {
        addHomepagePresenter()
        val items = completionAt("""
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function actionDefault(): void {
                    ${'$'}this->link('Homepage:<caret>');
                }
            }
        """.trimIndent())
        // actionDefault + renderDefault both exist, but "default" should appear once
        assertEquals(1, items.count { it == "default" })
    }

    fun testWorksInRedirect() {
        val items = completionAt("""
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function actionDefault(): void {}
                public function actionList(): void {
                    ${'$'}this->redirect('<caret>');
                }
            }
        """.trimIndent())
        assertContainsElements(items, "default", "list")
    }

    fun testNoCompletionInNonNetteClass() {
        val items = completionAt("""
            <?php
            class Router {
                public function redirect(string ${'$'}url): void {}
                public function doStuff(): void {
                    ${'$'}this->redirect('<caret>');
                }
            }
        """.trimIndent())
        assertEmpty(items)
    }
}
