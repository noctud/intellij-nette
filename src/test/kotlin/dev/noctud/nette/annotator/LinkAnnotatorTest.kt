package dev.noctud.nette.annotator

import dev.noctud.nette.BaseNetteTestCase

class LinkAnnotatorTest : BaseNetteTestCase() {

    fun testLinkStringIsHighlighted() {
        myFixture.configureByText("TestPresenter.php", """
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function actionDefault(): void {
                    ${'$'}this->link('detail');
                }
            }
        """.trimIndent())
        val highlights = myFixture.doHighlighting()
        val linkHighlight = highlights.find {
            it.text == "'detail'" && it.forcedTextAttributesKey == LinkAnnotator.LINK_DESTINATION
        }
        assertNotNull("Link string should be highlighted with LINK_DESTINATION", linkHighlight)
    }

    fun testRedirectStringIsHighlighted() {
        myFixture.configureByText("TestPresenter.php", """
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function actionDefault(): void {
                    ${'$'}this->redirect('Homepage:default');
                }
            }
        """.trimIndent())
        val highlights = myFixture.doHighlighting()
        val linkHighlight = highlights.find {
            it.text == "'Homepage:default'" && it.forcedTextAttributesKey == LinkAnnotator.LINK_DESTINATION
        }
        assertNotNull("Redirect string should be highlighted", linkHighlight)
    }

    fun testNonNetteClassNotHighlighted() {
        myFixture.configureByText("Router.php", """
            <?php
            class Router {
                public function redirect(string ${'$'}url): void {}
                public function test(): void {
                    ${'$'}this->redirect('something');
                }
            }
        """.trimIndent())
        val highlights = myFixture.doHighlighting()
        val linkHighlight = highlights.find {
            it.text == "'something'" && it.forcedTextAttributesKey == LinkAnnotator.LINK_DESTINATION
        }
        assertNull("Non-Nette class should NOT have link highlighting", linkHighlight)
    }

    fun testSecondParamNotHighlighted() {
        myFixture.configureByText("TestPresenter.php", """
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function actionDefault(): void {
                    ${'$'}this->link('detail', 'notALink');
                }
            }
        """.trimIndent())
        val highlights = myFixture.doHighlighting()
        val secondParam = highlights.find {
            it.text == "'notALink'" && it.forcedTextAttributesKey == LinkAnnotator.LINK_DESTINATION
        }
        assertNull("Second parameter should NOT be highlighted", secondParam)
    }
}
