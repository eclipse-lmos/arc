// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.filters

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.conversation.SystemMessage
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.dsl.withDSLContext
import org.eclipse.lmos.arc.assistants.support.TestBase
import org.eclipse.lmos.arc.core.Success
import org.junit.jupiter.api.Test

class UseCaseMatcherTest : TestBase() {

    @Test
    fun `formatConversationHistory includes user and assistant turns`() {
        val transcript = listOf(
            UserMessage("ich will umziehen"),
            AssistantMessage("Mobilfunk oder Festnetz?"),
            UserMessage("festnetz"),
            AssistantMessage("Sie können Ihren Umzugsauftrag hier stellen."),
        )

        val history = UseCaseMatcher.formatConversationHistory(transcript, maxMessages = 10)

        assertThat(history).isEqualTo(
            """
            User: ich will umziehen
            Assistant: Mobilfunk oder Festnetz?
            User: festnetz
            Assistant: Sie können Ihren Umzugsauftrag hier stellen.
            """.trimIndent(),
        )
    }

    @Test
    fun `formatConversationHistory limits to maxMessages`() {
        val transcript = listOf(
            UserMessage("one"),
            AssistantMessage("two"),
            UserMessage("three"),
            AssistantMessage("four"),
        )

        val history = UseCaseMatcher.formatConversationHistory(transcript, maxMessages = 2)

        assertThat(history).isEqualTo(
            """
            User: three
            Assistant: four
            """.trimIndent(),
        )
    }

    @Test
    fun `formatConversationHistory ignores system messages`() {
        val transcript = listOf(
            SystemMessage("You are helpful."),
            UserMessage("hello"),
            AssistantMessage("Hi there."),
        )

        val history = UseCaseMatcher.formatConversationHistory(transcript, maxMessages = 10)

        assertThat(history).isEqualTo(
            """
            User: hello
            Assistant: Hi there.
            """.trimIndent(),
        )
    }

    @Test
    fun `buildUserPrompt omits truncation note when full conversation fits`() {
        val transcript = listOf(
            UserMessage("ich will umziehen"),
            AssistantMessage("Mobilfunk oder Festnetz?"),
            UserMessage("festnetz"),
            AssistantMessage("Sie können Ihren Umzugsauftrag hier stellen."),
        )

        val prompt = UseCaseMatcher.buildUserPrompt(transcript, maxMessages = 10)

        assertThat(prompt).startsWith("Conversation:\n")
        assertThat(prompt).doesNotContain("earlier turns omitted")
    }

    @Test
    fun `buildUserPrompt includes truncation note when conversation is windowed`() {
        val transcript = listOf(
            UserMessage("one"),
            AssistantMessage("two"),
            UserMessage("three"),
            AssistantMessage("four"),
        )

        val prompt = UseCaseMatcher.buildUserPrompt(transcript, maxMessages = 2)

        assertThat(prompt).startsWith(
            "Conversation (last 2 user/assistant messages; earlier turns omitted):\n",
        )
        assertThat(prompt).contains("User: three")
        assertThat(prompt).contains("Assistant: four")
    }

    @Test
    fun `matchUseCase sends formatted conversation history to llm`() = runBlocking {
        withDSLContext(setOf(chatCompleterProvider)) {
            val messages = slot<List<ConversationMessage>>()
            coEvery { chatCompleter.complete(capture(messages), any(), any()) } answers {
                Success(AssistantMessage("<umzug_beauftragen_festnetz>"))
            }

            val transcript = listOf(
                UserMessage("ich will umziehen"),
                AssistantMessage("Mobilfunk oder Festnetz?"),
                UserMessage("festnetz"),
                AssistantMessage("Sie können Ihren Umzugsauftrag hier stellen."),
            )
            val useCases = "UseCase: umzug_beauftragen_festnetz\nSolution\nSubmit fixed-line relocation order."

            val result = UseCaseMatcher().matchUseCase(transcript, useCases, this)

            assertThat(result).isEqualTo("umzug_beauftragen_festnetz")
            coVerify { chatCompleter.complete(any(), any(), any()) }
            val userMessage = messages.captured.filterIsInstance<UserMessage>().single()
            assertThat(userMessage.content).contains("User: festnetz")
            assertThat(userMessage.content).contains("Assistant: Sie können Ihren Umzugsauftrag hier stellen.")
            assertThat(userMessage.content).contains("Conversation:")
            assertThat(userMessage.content).doesNotContain("earlier turns omitted")
            assertThat(userMessage.content).contains("Classify the latest assistant message")
        }
    }

    @Test
    fun `matchUseCase includes truncation note when conversation is windowed`() = runBlocking {
        withDSLContext(setOf(chatCompleterProvider)) {
            val messages = slot<List<ConversationMessage>>()
            coEvery { chatCompleter.complete(capture(messages), any(), any()) } answers {
                Success(AssistantMessage("<billing_issue>"))
            }

            val transcript = listOf(
                UserMessage("one"),
                AssistantMessage("two"),
                UserMessage("three"),
                AssistantMessage("Please review your invoice."),
            )
            val useCases = "UseCase: billing_issue\nSolution\nAsk the customer to review their invoice."

            UseCaseMatcher(maxMessages = 2).matchUseCase(transcript, useCases, this)

            val userMessage = messages.captured.filterIsInstance<UserMessage>().single()
            assertThat(userMessage.content).contains(
                "Conversation (last 2 user/assistant messages; earlier turns omitted):",
            )
            assertThat(userMessage.content).contains("User: three")
            assertThat(userMessage.content).doesNotContain("User: one")
        }
    }

    @Test
    fun `matchUseCase returns null when llm response does not match known use case`() = runBlocking {
        withDSLContext(setOf(chatCompleterProvider)) {
            coEvery { chatCompleter.complete(any(), any(), any()) } answers {
                Success(AssistantMessage("<unknown_use_case>"))
            }

            val result = UseCaseMatcher().matchUseCase(
                transcript = listOf(AssistantMessage("Hello")),
                useCases = "UseCase: billing_issue",
                context = this,
            )

            assertThat(result).isNull()
        }
    }
}
