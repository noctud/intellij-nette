package dev.noctud.nette.typeProvider

import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import dev.noctud.nette.BaseNetteTestCase

class ControlTypeProviderTest : BaseNetteTestCase() {

    private val typeProvider = ControlTypeProvider()

    fun testArrayAccessReturnsType() {
        myFixture.configureByText("TestPresenter.php", """
            <?php
            use Nette\Application\UI\Presenter;
            use Nette\Application\UI\Control;
            class GridControl extends Control {}
            class TestPresenter extends Presenter {
                protected function createComponentGrid(): GridControl {
                    return new GridControl();
                }
                public function actionDefault(): void {
                    ${'$'}this[<caret>'grid'];
                }
            }
        """.trimIndent())

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        // Walk up to find the ArrayAccessExpression
        var current = element
        while (current != null && current !is ArrayAccessExpression) {
            current = current.parent
        }
        assertNotNull("Should find ArrayAccessExpression", current)
        val type = typeProvider.getType(current!!)
        assertNotNull("Should return a type for array access", type)
    }

    fun testGetComponentReturnsType() {
        myFixture.configureByText("TestPresenter.php", """
            <?php
            use Nette\Application\UI\Presenter;
            use Nette\Application\UI\Control;
            class FormControl extends Control {}
            class TestPresenter extends Presenter {
                protected function createComponentForm(): FormControl {
                    return new FormControl();
                }
                public function actionDefault(): void {
                    ${'$'}this-><caret>getComponent('form');
                }
            }
        """.trimIndent())

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        var current = element
        while (current != null && current !is MethodReference) {
            current = current.parent
        }
        assertNotNull("Should find MethodReference", current)
        val methodRef = current as MethodReference
        assertEquals("getComponent", methodRef.name)
        val type = typeProvider.getType(methodRef)
        assertNotNull("Should return a type for getComponent", type)
    }

    fun testNonComponentMethodReturnsNull() {
        myFixture.configureByText("TestPresenter.php", """
            <?php
            use Nette\Application\UI\Presenter;
            class TestPresenter extends Presenter {
                public function actionDefault(): void {
                    ${'$'}this-><caret>doSomething('test');
                }
                public function doSomething(string ${'$'}s): void {}
            }
        """.trimIndent())

        val element = myFixture.file.findElementAt(myFixture.caretOffset)
        var current = element
        while (current != null && current !is MethodReference) {
            current = current.parent
        }
        if (current != null) {
            val type = typeProvider.getType(current)
            assertNull("Should not return type for non-getComponent method", type)
        }
    }

    fun testTypeProviderKey() {
        assertEquals('N', typeProvider.key)
    }
}
