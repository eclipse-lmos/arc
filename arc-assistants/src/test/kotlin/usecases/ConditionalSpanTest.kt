// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.dsl.extensions.local
import org.eclipse.lmos.arc.agents.dsl.withDSLContext
import org.junit.jupiter.api.Test

class ConditionalSpanTest {

    @Test
    fun `test parsing of conditional spans - no match`(): Unit = runBlocking {
        val conditionals = """<start>One
            Two
            Three</>""".split("\n").map { it.asConditional() }
        val output = StringBuilder()
        conditionals.output(conditions = emptySet(), output)
        Assertions.assertThat(output.toString()).isEqualTo("""""")
    }

    @Test
    fun `test parsing of conditional spans - match`(): Unit = runBlocking {
        val conditionals = """<start>
              One
              Two
              Three
            </>""".split("\n").map { it.asConditional() }
        val output = StringBuilder()
        conditionals.output(conditions = setOf("start"), output)
        Assertions.assertThat(output.toString()).isEqualTo(
            """
             
             One
             Two
             Three
             
            """.trimIndent(),
        )
    }

    @Test
    fun `test parsing of conditional use case`() = runBlocking {
        withDSLContext {
            val useCases = local("use_cases_conditionals.md")!!.toUseCases()
            val parsedUseCases = useCases.formatToString(conditions = setOf("on"))
            assertThat(parsedUseCases).isEqualTo(
                """
                ### UseCase: usecase1
                #### Description
                The description of the use case 1.
                
                #### Steps
                - Step on
                
                #### Solution
                Solution
                
                ----
                
                ### UseCase: usecase2
                #### Description
                The description of the use case 2.
                
                #### Solution
                Primary 1
                Primary 2
                end
                
                ----
                
                ### UseCase: usecase3
                #### Description
                The description of the use case 3.
                Second description.
                
                #### Solution
                Solution for use case 3
                
                ----
                
                
                """.trimIndent(),
            )
        }
    }
}
