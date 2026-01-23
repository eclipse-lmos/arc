// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.kotlin.runner
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.eclipse.lmos.arc.assistants.support.usecases.code.CodeBlock
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.getOrNull
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class KotlinCodeBlockRunnerTest {

    private val runner = KotlinCodeBlockRunner()

    @Test
    fun `canHandle returns true for kotlin language`() {
        val codeBlock = CodeBlock(code = "val x = 1", language = "kotlin")
        Assertions.assertThat(runner.canHandle(codeBlock)).isTrue
    }

    @Test
    fun `canHandle returns true for kts language`() {
        val codeBlock = CodeBlock(code = "val x = 1", language = "kts")
        Assertions.assertThat(runner.canHandle(codeBlock)).isTrue
    }

    @Test
    fun `canHandle returns true for Kotlin with uppercase`() {
        val codeBlock = CodeBlock(code = "val x = 1", language = "Kotlin")
        Assertions.assertThat(runner.canHandle(codeBlock)).isTrue
    }

    @Test
    fun `canHandle returns false for java language`() {
        val codeBlock = CodeBlock(code = "int x = 1;", language = "java")
        Assertions.assertThat(runner.canHandle(codeBlock)).isFalse
    }

    @Test
    fun `canHandle returns false for python language`() {
        val codeBlock = CodeBlock(code = "x = 1", language = "python")
        Assertions.assertThat(runner.canHandle(codeBlock)).isFalse
    }

    @Test
    fun `canHandle returns false for empty language`() {
        val codeBlock = CodeBlock(code = "val x = 1", language = "")
        Assertions.assertThat(runner.canHandle(codeBlock)).isFalse
    }

    @Test
    fun `run returns null for unsupported language`(): Unit = runBlocking {
        val codeBlock = CodeBlock(code = "int x = 1;", language = "java")
        val result = runner.run(codeBlock).getOrNull()
        Assertions.assertThat(result).isNull()
    }

    @Test
    fun `run executes simple kotlin expression`(): Unit = runBlocking {
        val codeBlock = CodeBlock(code = "1 + 1", language = "kotlin")
        val result = runner.run(codeBlock).getOrNull()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).isEqualTo("2")
    }

    @Test
    fun `run executes variable declaration and returns value`(): Unit = runBlocking {
        val codeBlock = CodeBlock(
            code = """
                val x = 10
                val y = 20
                x + y
            """.trimIndent(),
            language = "kotlin",
        )
        val result = runner.run(codeBlock).getOrNull()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).isEqualTo("30")
    }

    @Test
    fun `run executes function and returns result`(): Unit = runBlocking {
        val codeBlock = CodeBlock(
            code = """
                fun greet(name: String) = "Hello, ${'$'}name!"
                greet("World")
            """.trimIndent(),
            language = "kotlin",
        )
        val result = runner.run(codeBlock).getOrNull()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).isEqualTo("Hello, World!")
    }

    @Test
    fun `run handles script with no return value`(): Unit = runBlocking {
        val codeBlock = CodeBlock(
            code = """
                val x = 1
                Unit
            """.trimIndent(),
            language = "kotlin",
        )
        val result = runner.run(codeBlock).getOrNull()

        Assertions.assertThat(result).isEmpty()
    }

    @Test
    fun `run handles compilation error`(): Unit = runBlocking {
        val codeBlock = CodeBlock(
            code = "val x = ",
            language = "kotlin",
        )
        val result = runner.run(codeBlock) as Failure

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result.reason.message).contains("Compilation/Execution failed")
    }

    @Test
    fun `run handles runtime error`(): Unit = runBlocking {
        val codeBlock = CodeBlock(
            code = """
                val x: String? = null
                x!!.length
            """.trimIndent(),
            language = "kotlin",
        )
        val result = runner.run(codeBlock) as Failure

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result.reason.message).containsAnyOf("NullPointerException")
    }

    @Test
    fun `run handles division by zero`(): Unit = runBlocking {
        val codeBlock = CodeBlock(
            code = "10 / 0",
            language = "kotlin",
        )
        val result = runner.run(codeBlock) as Failure

        Assertions.assertThat(result).isNotNull
        // Division by zero in Kotlin throws ArithmeticException
        Assertions.assertThat(result.reason.message).containsAnyOf("ArithmeticException")
    }

    @Test
    fun `run executes list operations`(): Unit = runBlocking {
        val codeBlock = CodeBlock(
            code = """
                val numbers = listOf(1, 2, 3, 4, 5)
                numbers.filter { it % 2 == 0 }.sum()
            """.trimIndent(),
            language = "kotlin",
        )
        val result = runner.run(codeBlock).getOrNull()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).contains("6")
    }

    @Test
    fun `run executes string operations`(): Unit = runBlocking {
        val codeBlock = CodeBlock(
            code = """
                val text = "hello world"
                text.uppercase()
            """.trimIndent(),
            language = "kotlin",
        )
        val result = runner.run(codeBlock).getOrNull()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).contains("HELLO WORLD")
    }

    @Test
    fun `run executes when expression`(): Unit = runBlocking {
        val codeBlock = CodeBlock(
            code = """
                val x = 5
                when {
                    x < 0 -> "negative"
                    x > 0 -> "positive"
                    else -> "zero"
                }
            """.trimIndent(),
            language = "kotlin",
        )
        val result = runner.run(codeBlock).getOrNull()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).contains("positive")
    }

    @Test
    fun `run executes data class creation`(): Unit = runBlocking {
        val codeBlock = CodeBlock(
            code = """
                data class Person(val name: String, val age: Int)
                val person = Person("Alice", 30)
                person.name
            """.trimIndent(),
            language = "kotlin",
        )
        val result = runner.run(codeBlock).getOrNull()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).contains("Alice")
    }

    @Test
    fun `run executes map operations`(): Unit = runBlocking {
        val codeBlock = CodeBlock(
            code = """
                val map = mapOf("a" to 1, "b" to 2, "c" to 3)
                map.values.sum()
            """.trimIndent(),
            language = "kotlin",
        )
        val result = runner.run(codeBlock).getOrNull()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).contains("6")
    }

    @Test
    fun `run executes sequence operations`(): Unit = runBlocking {
        val codeBlock = CodeBlock(
            code = """
                (1..10).asSequence()
                    .filter { it % 2 == 0 }
                    .map { it * 2 }
                    .toList()
            """.trimIndent(),
            language = "kotlin",
        )
        val result = runner.run(codeBlock).getOrNull()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).contains("[4, 8, 12, 16, 20]")
    }

    fun `run respects timeout for long-running script`(): Unit = runBlocking {
        val runner = KotlinCodeBlockRunner()
        runner.timeout = 1.seconds
        val codeBlock = CodeBlock(
            code = """
                Thread.sleep(3000)
                "Done"
            """.trimIndent(),
            language = "kotlin",
        )
        val result = runner.run(codeBlock) as Failure

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result.reason.message).contains("Error")
    }

    @Test
    fun `run has access to time function`(): Unit = runBlocking {
        val runner = KotlinCodeBlockRunner()
        runner.timeout = 1.seconds
        val codeBlock = CodeBlock(
            code = """
              time()
            """.trimIndent(),
            language = "kotlin",
        )
        val result = runner.run(codeBlock).getOrNull()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).contains(":")
    }

    @Test
    fun `run executes nullable type handling`(): Unit = runBlocking {
        val codeBlock = CodeBlock(
            code = """
                val text: String? = "hello"
                text?.uppercase() ?: "default"
            """.trimIndent(),
            language = "kotlin",
        )
        val result = runner.run(codeBlock).getOrNull()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).contains("HELLO")
    }

    @Test
    fun `run executes lambda expressions`(): Unit = runBlocking {
        val codeBlock = CodeBlock(
            code = """
                val square: (Int) -> Int = { it * it }
                square(5)
            """.trimIndent(),
            language = "kotlin",
        )
        val result = runner.run(codeBlock).getOrNull()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).contains("25")
    }

    @Test
    fun `run executes extension function`(): Unit = runBlocking {
        val codeBlock = CodeBlock(
            code = """
                fun String.isPalindrome() = this == this.reversed()
                "racecar".isPalindrome()
            """.trimIndent(),
            language = "kotlin",
        )
        val result = runner.run(codeBlock).getOrNull()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).contains("true")
    }

    @Test
    fun `run executes sealed class`(): Unit = runBlocking {
        val codeBlock = CodeBlock(
            code = """
                sealed class Result {
                    data class Success(val value: Int) : Result()
                    data class Error(val message: String) : Result()
                }
                val result: Result = Result.Success(42)
                when (result) {
                    is Result.Success -> result.value
                    is Result.Error -> 0
                }
            """.trimIndent(),
            language = "kotlin",
        )
        val result = runner.run(codeBlock).getOrNull()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).contains("42")
    }

    @Test
    fun `run executes with let scope function`(): Unit = runBlocking {
        val codeBlock = CodeBlock(
            code = """
                "hello".let { it.uppercase() + "!" }
            """.trimIndent(),
            language = "kotlin",
        )
        val result = runner.run(codeBlock).getOrNull()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).contains("HELLO!")
    }

    @Test
    fun `run executes with also scope function`(): Unit = runBlocking {
        val codeBlock = CodeBlock(
            code = """
                val list = mutableListOf(1, 2, 3)
                list.also { it.add(4) }
            """.trimIndent(),
            language = "kotlin",
        )
        val result = runner.run(codeBlock).getOrNull()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).contains("[1, 2, 3, 4]")
    }

    @Test
    fun `run executes destructuring declaration`(): Unit = runBlocking {
        val codeBlock = CodeBlock(
            code = """
                val (a, b, c) = listOf(1, 2, 3)
                a + b + c
            """.trimIndent(),
            language = "kotlin",
        )
        val result = runner.run(codeBlock).getOrNull()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).contains("6")
    }

    @Test
    fun `run executes range operations`(): Unit = runBlocking {
        val codeBlock = CodeBlock(
            code = """
                (1..5).map { it * it }.sum()
            """.trimIndent(),
            language = "kotlin",
        )
        val result = runner.run(codeBlock).getOrNull()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).contains("55") // 1 + 4 + 9 + 16 + 25
    }

    @Test
    fun `run executes multiline string`(): Unit = runBlocking {
        val codeBlock = CodeBlock(
            code = """
                val text = ${"\"\"\""}
                    Hello
                    World
                ${"\"\"\""}
                text.trim()
            """.trimIndent(),
            language = "kotlin",
        )
        val result = runner.run(codeBlock).getOrNull()

        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result).contains("Hello")
        Assertions.assertThat(result).contains("World")
    }
}
