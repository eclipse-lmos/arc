// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.extensions

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.dsl.extensions.useCases
import org.eclipse.lmos.arc.agents.dsl.withDSLContext
import org.eclipse.lmos.arc.agents.memory.InMemoryMemory
import org.eclipse.lmos.arc.assistants.support.TestBase
import org.eclipse.lmos.arc.assistants.support.usecases.OutputOptions
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
            val result = useCases("use_cases.md", filter =  { useCase -> useCase.id != "usecase2" })
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
