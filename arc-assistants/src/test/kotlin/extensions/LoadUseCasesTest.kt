// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.extensions

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.dsl.extensions.getFallbackCases
import org.eclipse.lmos.arc.agents.dsl.extensions.useCases
import org.eclipse.lmos.arc.agents.dsl.withDSLContext
import org.eclipse.lmos.arc.agents.memory.InMemoryMemory
import org.eclipse.lmos.arc.assistants.support.TestBase
import org.eclipse.lmos.arc.assistants.support.usecases.OutputOptions
import org.eclipse.lmos.arc.assistants.support.usecases.UseCase
import org.junit.jupiter.api.Test

class LoadUseCasesTest : TestBase() {

    @Test
    fun `test use case function`(): Unit = runBlocking {
        withDSLContext(setOf(Conversation(), InMemoryMemory())) {
            val result = useCases("use_cases.md")
            assertThat(result.trim()).isEqualTo(
                """
                ### UseCase: usecase1
                #### Description
                The description of the use case 1.

                #### Steps
                - Step one of the use case 1.

                #### Solution
                Solution

                ----

                ### UseCase: usecase2
                #### Description
                The description of the use case 2.

                #### Solution
                Primary Solution

                ----

                ### UseCase: usecase3
                #### Description
                The description of the use case 3.

                #### Solution
                Solution for use case 3

                ----
                 
                """.trimIndent().trim(),
            )
        }
    }

    @Test
    fun `test use case filtering`(): Unit = runBlocking {
        withDSLContext(setOf(Conversation(), InMemoryMemory())) {
            val result = useCases("use_cases.md", filter = { useCase -> useCase.id != "usecase2" })
            assertThat(result).doesNotContain("usecase2")
        }
    }

    @Test
    fun `test use case output options`(): Unit = runBlocking {
        withDSLContext(setOf(Conversation(), InMemoryMemory())) {
            val result = useCases("use_cases.md", outputOptions = OutputOptions(outputSolution = false))
            assertThat(result.trim()).isEqualTo(
                """
                ### UseCase: usecase1
                #### Description
                The description of the use case 1.

              
                ----

                ### UseCase: usecase2
                #### Description
                The description of the use case 2.

              
                ----

                ### UseCase: usecase3
                #### Description
                The description of the use case 3.


                ----
                 
                """.trimIndent().trim(),
            )
        }
    }

}

class GetFallbackCasesTest {

    @Test
    fun `executionLimit positive - streak reached`() {
        val useCases = listOf(UseCase(id = "A", executionLimit = 3))
        val used = listOf("A", "A", "A", "B", "C")
        val result = getFallbackCases(used, useCases, fallbackLimit = 5)
        assert(result.contains("A"))
    }

    @Test
    fun `executionLimit negative - streak not reached`() {
        val useCases = listOf(UseCase(id = "A", executionLimit = 3))
        val used = listOf("A", "B", "A", "A", "C")
        val result = getFallbackCases(used, useCases, fallbackLimit = 5)
        assert(!result.contains("A"))
        assert(!result.contains("B"))
        assert(!result.contains("C"))
    }

    @Test
    fun `fallbackLimit positive - count reached`() {
        val useCases = listOf(UseCase(id = "B"))
        val used = listOf("B", "A", "B", "C", "B", "B")
        val result = getFallbackCases(used, useCases, fallbackLimit = 4)
        assert(result.contains("B"))
    }

    @Test
    fun `fallbackLimit negative - count not reached`() {
        val useCases = listOf(UseCase(id = "B"))
        val used = listOf("B", "A", "B", "C")
        val result = getFallbackCases(used, useCases, fallbackLimit = 4)
        assert(!result.contains("B"))
    }
}
