// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.assistants.support.usecases

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.eclipse.lmos.arc.agents.dsl.extensions.local
import org.eclipse.lmos.arc.agents.dsl.withDSLContext
import org.junit.jupiter.api.Test

class ConditionalStepsTest {

    @Test
    fun `test conditional steps - 1`() = runBlocking {
        withDSLContext {
            val useCases = local("use_cases_steps.md")!!.toUseCases()
            val parsedUseCases = useCases.formatToString(conditions = setOf("on"))
            Assertions.assertThat(parsedUseCases).isEqualTo(
                """
                ### UseCase: usecase1
                #### Description
                The description of the use case 1.
                
                #### Solution
                No step
                Step1
                
                ----
                
                
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `test conditional steps - 2`() = runBlocking {
        withDSLContext {
            val useCases = local("use_cases_steps.md")!!.toUseCases()
            val parsedUseCases = useCases.formatToString(conditions = setOf("on"), usedUseCases = listOf("usecase1"))
            Assertions.assertThat(parsedUseCases).isEqualTo(
                """
                ### UseCase: usecase1
                #### Description
                The description of the use case 1.
                
                #### Solution
                No step
                Step2
                
                ----
                
                
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `test conditional steps - 3`() = runBlocking {
        withDSLContext {
            val useCases = local("use_cases_steps.md")!!.toUseCases()
            val parsedUseCases =
                useCases.formatToString(conditions = setOf("on"), usedUseCases = listOf("usecase1", "usecase1"))
            Assertions.assertThat(parsedUseCases).isEqualTo(
                """
                ### UseCase: usecase1
                #### Description
                The description of the use case 1.
                
                #### Solution
                No step
                
                Step3
                
                ----
                
                
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `test conditional steps - 4`() = runBlocking {
        withDSLContext {
            val useCases = local("use_cases_steps.md")!!.toUseCases()
            val parsedUseCases = useCases.formatToString(usedUseCases = listOf("usecase1", "usecase1", "usecase1"))
            Assertions.assertThat(parsedUseCases).isEqualTo(
                """
                ### UseCase: usecase1
                #### Description
                The description of the use case 1.
                
                #### Solution
                No step
                Step4
                
                ----
                
                
                """.trimIndent(),
            )
        }
    }
}
