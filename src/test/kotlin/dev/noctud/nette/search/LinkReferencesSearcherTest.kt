package dev.noctud.nette.search

import com.intellij.psi.search.searches.ReferencesSearch
import com.jetbrains.php.PhpIndex
import dev.noctud.nette.BaseNetteTestCase

class LinkReferencesSearcherTest : BaseNetteTestCase() {

    fun testFindUsagesOfActionMethod() {
        myFixture.addFileToProject("HomepagePresenter.php", """
            <?php
            use Nette\Application\UI\Presenter;
            class HomepagePresenter extends Presenter {
                public function actionDetail(): void {}
            }
        """.trimIndent())
        myFixture.addFileToProject("OtherPresenter.php", """
            <?php
            use Nette\Application\UI\Presenter;
            class OtherPresenter extends Presenter {
                public function actionDefault(): void {
                    ${'$'}this->link('Homepage:detail');
                }
            }
        """.trimIndent())

        val method = PhpIndex.getInstance(project)
            .getClassesByName("HomepagePresenter").first()
            .findMethodByName("actionDetail")!!

        val refs = ReferencesSearch.search(method).findAll()
        assertTrue("Should find link string reference to actionDetail", refs.isNotEmpty())
    }

    fun testFindUsagesOfSignalMethod() {
        myFixture.addFileToProject("TestPresenter.php", """
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function handleRefresh(): void {}
                public function actionDefault(): void {
                    ${'$'}this->link('refresh!');
                }
            }
        """.trimIndent())

        val method = PhpIndex.getInstance(project)
            .getClassesByName("TestPresenter").first()
            .findMethodByName("handleRefresh")!!

        val refs = ReferencesSearch.search(method).findAll()
        assertTrue("Should find link string reference to handleRefresh", refs.isNotEmpty())
    }

    fun testLinkStringNotModifiedDuringRename() {
        myFixture.addFileToProject("HomepagePresenter.php", """
            <?php
            use Nette\Application\UI\Presenter;
            class HomepagePresenter extends Presenter {
                public function actionDetail(): void {}
            }
        """.trimIndent())
        myFixture.addFileToProject("OtherPresenter.php", """
            <?php
            use Nette\Application\UI\Presenter;
            class OtherPresenter extends Presenter {
                public function actionDefault(): void {
                    ${'$'}this->link('Homepage:detail');
                }
            }
        """.trimIndent())

        val method = PhpIndex.getInstance(project)
            .getClassesByName("HomepagePresenter").first()
            .findMethodByName("actionDetail")!!

        val refs = ReferencesSearch.search(method).findAll()
        for (ref in refs) {
            // handleElementRename should be a no-op — string stays unchanged
            val result = ref.handleElementRename("actionRenamed")
            assertEquals("Link string should not be modified", ref.element, result)
        }
    }

    fun testNoFalsePositivesForNonNetteMethod() {
        myFixture.addFileToProject("Service.php", """
            <?php
            class Service {
                public function actionProcess(): void {}
            }
        """.trimIndent())
        myFixture.addFileToProject("Usage.php", """
            <?php
            class Usage {
                public function test(): void {
                    ${'$'}x = 'process';
                }
            }
        """.trimIndent())

        val method = PhpIndex.getInstance(project)
            .getClassesByName("Service").first()
            .findMethodByName("actionProcess")!!

        val refs = ReferencesSearch.search(method).findAll()
        val linkRefs = refs.filter { it.element.containingFile?.name == "Usage.php" }
        assertTrue("Should not find false positive references in non-Nette context", linkRefs.isEmpty())
    }
}
