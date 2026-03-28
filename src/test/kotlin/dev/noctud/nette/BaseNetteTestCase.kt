package dev.noctud.nette

import com.intellij.testFramework.fixtures.BasePlatformTestCase

abstract class BaseNetteTestCase : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        addNetteStubs()
    }

    private fun addNetteStubs() {
        myFixture.addFileToProject(
            "nette/Component.php", """
            <?php
            namespace Nette\Application\UI;
            class Component {
                public function link(string ${'$'}destination): string { return ''; }
                public function lazyLink(string ${'$'}destination): object { return new \stdClass; }
                public function isLinkCurrent(string ${'$'}destination): bool { return false; }
            }
        """.trimIndent()
        )

        myFixture.addFileToProject(
            "nette/Control.php", """
            <?php
            namespace Nette\Application\UI;
            class Control extends Component {}
        """.trimIndent()
        )

        myFixture.addFileToProject(
            "nette/Presenter.php", """
            <?php
            namespace Nette\Application\UI;
            class Presenter extends Control {
                public function redirect(string ${'$'}destination): void {}
                public function redirectPermanent(string ${'$'}destination): void {}
                public function forward(string ${'$'}destination): void {}
                public function canonicalize(?string ${'$'}destination = null): void {}
            }
        """.trimIndent()
        )
    }
}
