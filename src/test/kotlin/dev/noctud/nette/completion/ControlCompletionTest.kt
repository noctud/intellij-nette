package dev.noctud.nette.completion

import dev.noctud.nette.BaseNetteTestCase

class ControlCompletionTest : BaseNetteTestCase() {

    private fun completionAt(code: String): List<String> {
        myFixture.configureByText("TestPresenter.php", code)
        val completions = myFixture.completeBasic()
        return completions?.map { it.lookupString } ?: emptyList()
    }

    fun testComponentNamesInGetComponent() {
        val items = completionAt("""
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function actionDefault(): void {
                    ${'$'}this->getComponent('<caret>');
                }
                protected function createComponentGrid(): \Nette\Application\UI\Control {
                    return new \Nette\Application\UI\Control();
                }
                protected function createComponentForm(): \Nette\Application\UI\Control {
                    return new \Nette\Application\UI\Control();
                }
            }
        """.trimIndent())
        assertContainsElements(items, "grid", "form")
    }

    fun testComponentNamesInArrayAccess() {
        val items = completionAt("""
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function actionDefault(): void {
                    ${'$'}this['<caret>'];
                }
                protected function createComponentMenu(): \Nette\Application\UI\Control {
                    return new \Nette\Application\UI\Control();
                }
            }
        """.trimIndent())
        assertContainsElements(items, "menu")
    }

    fun testNoCompletionInNonNetteClass() {
        val items = completionAt("""
            <?php
            class NotAComponent {
                public function getComponent(string ${'$'}name): object { return new \stdClass; }
                public function test(): void {
                    ${'$'}this->getComponent('<caret>');
                }
                protected function createComponentFoo(): object { return new \stdClass; }
            }
        """.trimIndent())
        assertDoesntContain(items, "foo")
    }
}
