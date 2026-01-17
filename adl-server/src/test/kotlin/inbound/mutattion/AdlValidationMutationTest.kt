// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AdlValidationMutationTest {

    private val mutation = AdlValidationMutation()

    @Test
    fun `test validation with valid ADL`() = runBlocking {
        val validAdl = """
            ### UseCase: password_reset
            #### Description
            User wants to reset their password.

            #### Solution
            Call @reset_password() and go to use case #user_verification.
            
            ----
        """.trimIndent()

        val result = mutation.validate(validAdl)

        assertThat(result.syntaxErrors).isEmpty()
        assertThat(result.usedTools).contains("reset_password")
        assertThat(result.references).contains("user_verification")
    }

    @Test
    fun `test validation detects syntax errors`() = runBlocking {
        val invalidAdl = """
            ### UseCase: test
            #### Solution
            This has an unclosed bracket [
            
            ----
        """.trimIndent()

        val result = mutation.validate(invalidAdl)

        assertThat(result.syntaxErrors).isNotEmpty
        assertThat(result.syntaxErrors.any { it.message.contains("Unclosed") }).isTrue
    }

    @Test
    fun `test validation extracts multiple tools`() = runBlocking {
        val adl = """
            ### UseCase: multi_tool
            #### Solution
            Call @tool1() and @tool2() then use @tool3().
            
            ----
        """.trimIndent()

        val result = mutation.validate(adl)

        assertThat(result.usedTools).containsExactlyInAnyOrder("tool1", "tool2", "tool3")
    }

    @Test
    fun `test validation extracts multiple references`() = runBlocking {
        val adl = """
            ### UseCase: multi_ref
            #### Solution
            Go to #usecase1 and then #usecase2.
            Check https://example.com for details.
            
            ----
        """.trimIndent()

        val result = mutation.validate(adl)

        assertThat(result.references).contains("usecase1", "usecase2")
        assertThat(result.references.any { it.contains("example.com") }).isTrue
    }

    @Test
    fun `test validation detects unclosed quotes`() = runBlocking {
        val adl = """
            ### UseCase: quotes
            #### Solution
            This has an unclosed "quote
            
            ----
        """.trimIndent()

        val result = mutation.validate(adl)

        assertThat(result.syntaxErrors).isNotEmpty
        assertThat(result.syntaxErrors.any { it.message.contains("quote") }).isTrue
    }

    @Test
    fun `test validation with parsing error still extracts tools and references`() = runBlocking {
        val malformedAdl = """
            ### UseCase: broken
            #### Solution
            Call @extract_tool() and reference #some_usecase.
            [This bracket is never closed
            
        """.trimIndent()

        val result = mutation.validate(malformedAdl)

        // Should have syntax errors
        assertThat(result.syntaxErrors).isNotEmpty
        // But should still extract tools and references
        assertThat(result.usedTools).contains("extract_tool")
        assertThat(result.references).contains("some_usecase")
    }

    @Test
    fun `test validation with empty ADL`() = runBlocking {
        val result = mutation.validate("")

        assertThat(result.syntaxErrors).isEmpty()
        assertThat(result.usedTools).isEmpty()
        assertThat(result.references).isEmpty()
    }

    @Test
    fun `test validation detects mixed tabs and spaces`() = runBlocking {
        val adl = """
            ### UseCase: indentation
            #### Solution
            \tThis line uses tabs
                This line uses spaces
            
            ----
        """.trimIndent()

        val result = mutation.validate(adl)

        assertThat(result.syntaxErrors).isNotEmpty
        assertThat(result.syntaxErrors.any { it.message.contains("Mixed tabs and spaces") }).isTrue
    }
}

