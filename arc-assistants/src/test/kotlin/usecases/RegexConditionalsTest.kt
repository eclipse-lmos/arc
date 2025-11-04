// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.assistants.support.usecases

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RegexConditionalsTest {

    @Test
    fun `returns empty set when no regex conditions present`() {
        val useCase = UseCase(
            id = "test",
            conditions = setOf("foo"),
            steps = listOf(),
            solution = listOf(),
        )
        val result = useCase.regexConditionals("some text")
        assertThat(result).isEmpty()
    }

    @Test
    fun `returns pattern when regex condition matches`() {
        val useCase = UseCase(
            id = "test",
            conditions = setOf("regex:.*important.*"),
            steps = listOf(),
            solution = listOf(),
        )
        val result = useCase.regexConditionals("This is very important!")
        assertThat(result).containsExactly("regex:.*important.*")
    }

    @Test
    fun `returns empty set when regex condition does not match`() {
        val useCase = UseCase(
            id = "test",
            conditions = setOf("regex:.*urgent.*"),
            steps = listOf(),
            solution = listOf(),
        )
        val result = useCase.regexConditionals("This is not so important.")
        assertThat(result).isEmpty()
    }

    @Test
    fun `finds regex conditions also in steps and solution`() {
        val useCase = UseCase(
            id = "test",
            steps = listOf(Conditional(text = "foo", conditions = setOf("regex:abc.*"))),
            solution = listOf(Conditional(text = "bar", conditions = setOf("regex:xyz.*"))),
        )
        val result = useCase.regexConditionals("abc123 and xyz789")
        assertThat(result).containsExactlyInAnyOrder("regex:abc.*", "regex:xyz.*")
    }

    @Test
    fun `ignores non-regex conditions`() {
        val useCase = UseCase(
            id = "test",
            conditions = setOf("foo", "regex:bar.*"),
            steps = listOf(Conditional(text = "baz", conditions = setOf("qux"))),
        )
        val result = useCase.regexConditionals("bar123")
        assertThat(result).containsExactly("regex:bar.*")
    }

    @Test
    fun `test regex conditional in solution - match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <regex:.*bar.*>Bar
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "I am foobar!").trim()
        assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |Bar
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test regex conditional in solution - no match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <regex:.*bar.*>Bar
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "I am ok!").trim()
        assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }
}
