package dev.noctud.nette.typeProvider

import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.MemberReference
import com.jetbrains.php.lang.psi.elements.PhpTypedElement
import dev.noctud.nette.BaseNetteTestCase

class TemplateTypeProviderTest : BaseNetteTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "nette/Template.php", """
            <?php
            namespace Nette\Application\UI {
                interface Template {}
            }
            namespace Nette\Bridges\ApplicationLatte {
                abstract class Template implements \Nette\Application\UI\Template {}
            }
        """.trimIndent()
        )
    }

    private fun addProductTemplates() {
        myFixture.addFileToProject(
            "app/UI/Product/ProductTemplate.php", """
            <?php
            namespace App\UI\Product;
            use Nette\Bridges\ApplicationLatte\Template;
            class ProductTemplate extends Template {}
            class ProductEditTemplate extends ProductTemplate {}
        """.trimIndent()
        )
    }

    /** The types of the member reference at the caret, as PhpStorm completes them. */
    private fun typesAtCaret(code: String): Set<String> {
        myFixture.configureByText("ProductPresenter.php", code)
        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        val reference = PsiTreeUtil.getParentOfType(element, MemberReference::class.java)
        assertNotNull("Should find a member reference at the caret", reference)
        return (reference as PhpTypedElement).globalType.types
    }

    fun testTemplateOfPresenter() {
        addProductTemplates()
        val types = typesAtCaret("""
            <?php
            namespace App\UI\Product;
            class ProductPresenter extends \Nette\Application\UI\Presenter {
                public function renderDefault(): void {
                    ${'$'}this->temp<caret>late;
                }
            }
        """.trimIndent())

        assertContainsElements(types, "\\App\\UI\\Product\\ProductTemplate")
        assertDoesntContain(types, "\\App\\UI\\Product\\ProductEditTemplate")
    }

    fun testTemplateOfAction() {
        addProductTemplates()
        val types = typesAtCaret("""
            <?php
            namespace App\UI\Product;
            class ProductPresenter extends \Nette\Application\UI\Presenter {
                public function actionEdit(): void {
                    ${'$'}this->temp<caret>late;
                }
            }
        """.trimIndent())

        assertContainsElements(types, "\\App\\UI\\Product\\ProductEditTemplate")
    }

    fun testTemplateOfRenderMethod() {
        addProductTemplates()
        val types = typesAtCaret("""
            <?php
            namespace App\UI\Product;
            class ProductPresenter extends \Nette\Application\UI\Presenter {
                public function renderEdit(): void {
                    ${'$'}this->temp<caret>late;
                }
            }
        """.trimIndent())

        assertContainsElements(types, "\\App\\UI\\Product\\ProductEditTemplate")
    }

    fun testTemplateOfPresenterOutsideAction() {
        addProductTemplates()
        val types = typesAtCaret("""
            <?php
            namespace App\UI\Product;
            class ProductPresenter extends \Nette\Application\UI\Presenter {
                public function beforeRender(): void {
                    ${'$'}this->temp<caret>late;
                }
            }
        """.trimIndent())

        assertContainsElements(types, "\\App\\UI\\Product\\ProductTemplate")
        assertDoesntContain(types, "\\App\\UI\\Product\\ProductEditTemplate")
    }

    fun testGetTemplate() {
        addProductTemplates()
        val types = typesAtCaret("""
            <?php
            namespace App\UI\Product;
            class ProductPresenter extends \Nette\Application\UI\Presenter {
                public function renderDefault(): void {
                    ${'$'}this->getTemp<caret>late();
                }
            }
        """.trimIndent())

        assertContainsElements(types, "\\App\\UI\\Product\\ProductTemplate")
    }

    fun testPropertiesOfTemplateAreCompleted() {
        myFixture.addFileToProject(
            "app/UI/Product/ProductTemplate.php", """
            <?php
            namespace App\UI\Product;
            class ProductTemplate extends \Nette\Bridges\ApplicationLatte\Template {
                public array ${'$'}products;
                public int ${'$'}productCount;
            }
        """.trimIndent()
        )
        myFixture.configureByText("ProductPresenter.php", """
            <?php
            namespace App\UI\Product;
            class ProductPresenter extends \Nette\Application\UI\Presenter {
                public function renderDefault(): void {
                    ${'$'}this->template->prod<caret>
                }
            }
        """.trimIndent())

        myFixture.completeBasic()

        assertContainsElements(myFixture.lookupElementStrings!!, "products", "productCount")
    }

    fun testTemplateOfControl() {
        val types = typesAtCaret("""
            <?php
            namespace App\UI\Menu;
            use Nette\Bridges\ApplicationLatte\Template;
            class MenuTemplate extends Template {}
            class MenuControl extends \Nette\Application\UI\Control {
                public function render(): void {
                    ${'$'}this->temp<caret>late;
                }
            }
        """.trimIndent())

        assertContainsElements(types, "\\App\\UI\\Menu\\MenuTemplate")
    }

    fun testClassThatIsNotTemplateIsIgnored() {
        val types = typesAtCaret("""
            <?php
            namespace App\UI\Product;
            class ProductTemplate {}
            class ProductPresenter extends \Nette\Application\UI\Presenter {
                public function renderDefault(): void {
                    ${'$'}this->temp<caret>late;
                }
            }
        """.trimIndent())

        assertDoesntContain(types, "\\App\\UI\\Product\\ProductTemplate")
    }

    fun testTemplateOfOtherObjectIsIgnored() {
        addProductTemplates()
        val types = typesAtCaret("""
            <?php
            namespace App\UI\Product;
            class ProductPresenter extends \Nette\Application\UI\Presenter {
                public function renderDefault(\stdClass ${'$'}other): void {
                    ${'$'}other->temp<caret>late;
                }
            }
        """.trimIndent())

        assertDoesntContain(types, "\\App\\UI\\Product\\ProductTemplate")
    }

    fun testNoTemplateOutsideComponent() {
        addProductTemplates()
        val types = typesAtCaret("""
            <?php
            namespace App\UI\Product;
            class Product {
                public function render(): void {
                    ${'$'}this->temp<caret>late;
                }
            }
        """.trimIndent())

        assertDoesntContain(types, "\\App\\UI\\Product\\ProductTemplate")
    }
}
