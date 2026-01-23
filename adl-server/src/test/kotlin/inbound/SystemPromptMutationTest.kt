// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.adl.server.sessions.InMemorySessions
import org.eclipse.lmos.adl.server.sessions.Sessions
import org.eclipse.lmos.adl.server.templates.TemplateLoader
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SystemPromptMutationTest {

    private lateinit var sessions: Sessions
    private lateinit var templateLoader: TemplateLoader
    private lateinit var mutation: SystemPromptMutation

    @BeforeEach
    fun setUp() {
        sessions = InMemorySessions()
        templateLoader = TemplateLoader()
        mutation = SystemPromptMutation(sessions, templateLoader)
    }

    @Test
    fun `systemPrompt generates prompt without session`() {
        runBlocking {
            val adl = """
                ### UseCase: greeting
                #### Description
                Customer wants to greet the assistant.
                
                #### Solution
                Say hello back to the customer.
                ----
            """.trimIndent()

            val result = mutation.systemPrompt(adl)

            assertThat(result.useCaseCount).isEqualTo(1)
            assertThat(result.turn).isNull()
            assertThat(result.systemPrompt).contains("greeting")
            assertThat(result.systemPrompt).contains("Customer wants to greet the assistant")
            assertThat(result.systemPrompt).contains("Say hello back to the customer")
        }
    }

    @Test
    fun `systemPrompt generates prompt with session and increments turn`() {
        runBlocking {
            val adl = """
                ### UseCase: test_case
                #### Description
                Test description.
                
                #### Solution
                Test solution.
                ----
            """.trimIndent()
            val sessionId = "test-session-123"

            val result1 = mutation.systemPrompt(adl, sessionId = sessionId)
            val result2 = mutation.systemPrompt(adl, sessionId = sessionId)
            val result3 = mutation.systemPrompt(adl, sessionId = sessionId)

            assertThat(result1.turn).isEqualTo(1)
            assertThat(result2.turn).isEqualTo(2)
            assertThat(result3.turn).isEqualTo(3)
            assertThat(result1.useCaseCount).isEqualTo(1)
        }
    }

    @Test
    fun `systemPrompt filters use cases with conditionals`() {
        runBlocking {
            val adl = """
                ### UseCase: mobile_case <mobile>
                #### Description
                Mobile-specific use case.
                
                #### Solution
                Handle mobile request.
                ----
                
                ### UseCase: general_case
                #### Description
                General use case.
                
                #### Solution
                Handle general request.
                ----
            """.trimIndent()

            val resultWithMobile = mutation.systemPrompt(adl, conditionals = listOf("mobile"))
            val resultWithoutMobile = mutation.systemPrompt(adl, conditionals = emptyList())

            // Both use cases are parsed
            assertThat(resultWithMobile.useCaseCount).isEqualTo(2)
            assertThat(resultWithoutMobile.useCaseCount).isEqualTo(2)

            // With mobile conditional, both should be in output
            assertThat(resultWithMobile.systemPrompt).contains("mobile_case")
            assertThat(resultWithMobile.systemPrompt).contains("general_case")

            // Without mobile conditional, only general case should be in output
            assertThat(resultWithoutMobile.systemPrompt).doesNotContain("mobile_case")
            assertThat(resultWithoutMobile.systemPrompt).contains("general_case")
        }
    }

    @Test
    fun `systemPrompt includes role and time in output`() {
        runBlocking {
            val adl = """
                ### UseCase: simple_case
                #### Description
                Simple description.
                
                #### Solution
                Simple solution.
                ----
            """.trimIndent()

            val result = mutation.systemPrompt(adl)

            // Should contain role information from role.md template
            assertThat(result.systemPrompt).contains("customer support agent")
            assertThat(result.systemPrompt).contains("Role and Responsibilities")
        }
    }

    @Test
    fun `systemPrompt handles empty ADL`() {
        runBlocking {
            val adl = ""

            val result = mutation.systemPrompt(adl)

            assertThat(result.useCaseCount).isEqualTo(0)
            assertThat(result.systemPrompt).isNotEmpty()
        }
    }

    @Test
    fun `systemPrompt with different sessions tracks turns independently`() {
        runBlocking {
            val adl = """
                ### UseCase: test
                #### Description
                Test.
                
                #### Solution
                Test.
                ----
            """.trimIndent()

            val result1Session1 = mutation.systemPrompt(adl, sessionId = "session-1")
            val result1Session2 = mutation.systemPrompt(adl, sessionId = "session-2")
            val result2Session1 = mutation.systemPrompt(adl, sessionId = "session-1")

            assertThat(result1Session1.turn).isEqualTo(1)
            assertThat(result1Session2.turn).isEqualTo(1)
            assertThat(result2Session1.turn).isEqualTo(2)
        }
    }

    @Test
    fun `systemPrompt handles multiple use cases`() {
        runBlocking {
            val adl = """
                ### UseCase: case_one
                #### Description
                First use case.
                
                #### Solution
                Solution one.
                ----
                
                ### UseCase: case_two
                #### Description
                Second use case.
                
                #### Solution
                Solution two.
                ----
                
                ### UseCase: case_three
                #### Description
                Third use case.
                
                #### Solution
                Solution three.
                ----
            """.trimIndent()

            val result = mutation.systemPrompt(adl)

            assertThat(result.useCaseCount).isEqualTo(3)
            assertThat(result.systemPrompt).contains("case_one")
            assertThat(result.systemPrompt).contains("case_two")
            assertThat(result.systemPrompt).contains("case_three")
        }
    }

    @Test
    fun `systemPrompt applies multiple conditionals`() {
        runBlocking {
            val adl = """
                ### UseCase: mobile_case <mobile>
                #### Description
                Mobile only.
                
                #### Solution
                Mobile solution.
                ----
                
                ### UseCase: premium_case <premium>
                #### Description
                Premium only.
                
                #### Solution
                Premium solution.
                ----
                
                ### UseCase: general_case
                #### Description
                General case.
                
                #### Solution
                General solution.
                ----
            """.trimIndent()

            val resultBothConditions = mutation.systemPrompt(
                adl,
                conditionals = listOf("mobile", "premium"),
            )
            val resultMobileOnly = mutation.systemPrompt(
                adl,
                conditionals = listOf("mobile"),
            )

            // With both conditionals, all cases should appear
            assertThat(resultBothConditions.systemPrompt).contains("mobile_case")
            assertThat(resultBothConditions.systemPrompt).contains("premium_case")
            assertThat(resultBothConditions.systemPrompt).contains("general_case")

            // With only mobile, premium case should not appear
            assertThat(resultMobileOnly.systemPrompt).contains("mobile_case")
            assertThat(resultMobileOnly.systemPrompt).doesNotContain("premium_case")
            assertThat(resultMobileOnly.systemPrompt).contains("general_case")
        }
    }
}
