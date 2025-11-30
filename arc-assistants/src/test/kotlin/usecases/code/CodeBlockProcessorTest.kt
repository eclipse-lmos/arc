// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases.code

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CodeBlockProcessorTest {

    private val processor = CodeBlockProcessor()

    @Test
    fun `process returns original string when no code blocks present`(): Unit = runBlocking {
        val input = """
            This is a simple string
            with no code blocks.
            Just plain text.
        """.trimIndent()

        val result = processor.process(input)
        assertThat(result).isEqualTo(input)
    }

    @Test
    fun `process executes single kotlin code block`(): Unit = runBlocking {
        val input = """
            Here is some code:
            ```kotlin
            1 + 1
            ```
            End of text.
        """.trimIndent()

        val result = processor.process(input)

        assertThat(result).contains("2")
        assertThat(result).doesNotContain("1 + 1")
    }

    @Test
    fun `process executes multiple kotlin code blocks`(): Unit = runBlocking {
        val input = """
            First calculation:
            ```kotlin
            2 * 3
            ```
            
            Second calculation:
            ```kotlin
            10 - 5
            ```
        """.trimIndent()

        val result = processor.process(input)

        assertThat(result).contains("6")
        assertThat(result).contains("5")
    }

    @Test
    fun `process preserves code block with unsupported language`(): Unit = runBlocking {
        val input = """
            Python code (not supported):
            ```python
            print("hello")
            ```
        """.trimIndent()

        val result = processor.process(input)

        // Should remain unchanged since no Python runner
        assertThat(result).contains("```python")
        assertThat(result).contains("print(\"hello\")")
    }

    @Test
    fun `process handles mixed supported and unsupported languages`(): Unit = runBlocking {
        val input = """
            Kotlin code:
            ```kotlin
            5 + 5
            ```
            
            Java code (not supported):
            ```java
            int x = 10;
            ```
        """.trimIndent()

        val result = processor.process(input)

        // Kotlin should be executed
        assertThat(result).contains("10")
        assertThat(result).doesNotContain("5 + 5")

        // Java should remain unchanged
        assertThat(result).contains("```java")
        assertThat(result).contains("int x = 10;")
    }

    @Test
    fun `process handles code block with empty language`(): Unit = runBlocking {
        val input = """
            Code without language:
            ```
            some text
            ```
        """.trimIndent()

        val result = processor.process(input)

        // Should remain unchanged - no language specified
        assertThat(result).contains("```")
        assertThat(result).contains("some text")
    }

    @Test
    fun `process handles complex kotlin code with multiple lines`(): Unit = runBlocking {
        val input = """
            Calculate sum:
            ```kotlin
            val numbers = listOf(1, 2, 3, 4, 5)
            numbers.sum()
            ```
        """.trimIndent()

        val result = processor.process(input)

        assertThat(result).contains("15")
    }

    @Test
    fun `process handles code block with indentation`(): Unit = runBlocking {
        val input = """
            Some text:
                ```kotlin
                10 * 2
                ```
        """.trimIndent()

        val result = processor.process(input)

        assertThat(result).contains("20")
    }

    @Test
    fun `process handles kts language identifier`(): Unit = runBlocking {
        val input = """
            Kotlin script:
            ```kts
            3 * 7
            ```
        """.trimIndent()

        val result = processor.process(input)

        assertThat(result).contains("21")
    }

    @Test
    fun `process handles code block with compilation error`(): Unit = runBlocking {
        val input = """
            Bad code:
            ```kotlin
            val x =
            ```
        """.trimIndent()

        try {
            processor.process(input)
        } catch (_: CodeException) {
            return@runBlocking
        }
        assertTrue(false)
    }

    @Test
    fun `process handles consecutive code blocks`(): Unit = runBlocking {
        val input = """
            ```kotlin
            1 + 1
            ```
            ```kotlin
            2 + 2
            ```
        """.trimIndent()

        val result = processor.process(input)

        assertThat(result).contains("2")
        assertThat(result).contains("4")
    }

    @Test
    fun `process handles code block at start of string`(): Unit = runBlocking {
        val input = """```kotlin
            7 * 3
            ```
            Text after code.
        """.trimIndent()

        val result = processor.process(input)

        assertThat(result).contains("21")
    }

    @Test
    fun `process handles code block at end of string`(): Unit = runBlocking {
        val input = """
            Text before code.
            ```kotlin
            9 - 4
            ```
        """.trimIndent()

        val result = processor.process(input)

        assertThat(result).contains("5")
    }

    @Test
    fun `canHandleLanguage returns true for kotlin`() {
        assertThat(processor.canHandleLanguage("kotlin")).isTrue
    }

    @Test
    fun `canHandleLanguage returns true for kts`() {
        assertThat(processor.canHandleLanguage("kts")).isTrue
    }

    @Test
    fun `canHandleLanguage returns false for unsupported language`() {
        assertThat(processor.canHandleLanguage("python")).isFalse
        assertThat(processor.canHandleLanguage("java")).isFalse
    }

    @Test
    fun `process handles code block with string operations`(): Unit = runBlocking {
        val input = """
            String test:
            ```kotlin
            "hello".uppercase()
            ```
        """.trimIndent()

        val result = processor.process(input)

        assertThat(result).contains("HELLO")
    }

    @Test
    fun `process handles code block with list operations`(): Unit = runBlocking {
        val input = """
            List operations:
            ```kotlin
            listOf(2, 4, 6).filter { it > 3 }
            ```
        """.trimIndent()

        val result = processor.process(input)

        assertThat(result).contains("[4, 6]")
    }

    @Test
    fun `process preserves text around code blocks`(): Unit = runBlocking {
        val input = """
            Start text here.
            ```kotlin
            50 / 5
            ```
            Middle text here.
            ```kotlin
            8 + 2
            ```
            End text here.
        """.trimIndent()

        val result = processor.process(input)

        assertThat(result).contains("Start text here")
        assertThat(result).contains("Middle text here")
        assertThat(result).contains("End text here")
        assertThat(result).contains("10")
    }

    @Test
    fun `process handles empty code block`(): Unit = runBlocking {
        val input = """
            Empty block:
            ```kotlin
            ```
        """.trimIndent()

        val result = processor.process(input)

        assertThat(result).isEqualTo(
            """
           Empty block:
         
            """.trimIndent(),
        )
    }

    @Test
    fun `process handles code block with Unit return`(): Unit = runBlocking {
        val input = """
            Unit return:
            ```kotlin
            val x = 5
            Unit
            ```
        """.trimIndent()

        val result = processor.process(input)

        assertThat(result).isEqualTo(
            """
            Unit return:
          
            """.trimIndent(),
        )
    }
}
