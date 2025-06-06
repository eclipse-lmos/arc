// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.agent

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.RetrySignal
import org.eclipse.lmos.arc.agents.TestBase
import org.eclipse.lmos.arc.agents.WithConversationResult
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.conversation.toConversation
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.core.Success
import org.junit.jupiter.api.Test

class AgentRecoveryTest : TestBase() {

    @Test
    fun testRecoverAgentFailureWithWithConversationResultException() {
        runBlocking {
            // Given
            val agent = mockk<ConversationAgent>()
            coEvery { agent.name } returns "TestAgent"
            val dslContext = mockk<DSLContext>()
            val input = "test input".toConversation()
            val conversation = Conversation(transcript = listOf(UserMessage("test"), AssistantMessage("test response")))
            val error = object : Exception(), WithConversationResult {
                override val conversation = conversation
            }

            // When
            val result = agent.recoverAgentFailure(
                error = error,
                dslContext = dslContext,
                input = input,
                context = emptySet(),
                onFail = { null },
            )

            // Then
            assertThat(result).isNotNull
            assertThat(result!!.first).isEqualTo(conversation)
            assertThat(result.second).isTrue()
        }
    }

    @Test
    fun testRecoverAgentFailureWithWithConversationResultAsCause() {
        runBlocking {
            // Given
            val agent = mockk<ConversationAgent>()
            coEvery { agent.name } returns "TestAgent"
            val dslContext = mockk<DSLContext>()
            val input = "test input".toConversation()
            val conversation = Conversation(transcript = listOf(UserMessage("test"), AssistantMessage("test response")))
            val cause = object : Exception(), WithConversationResult {
                override val conversation = conversation
            }
            val error = Exception("Wrapper exception", cause)

            // When
            val result = agent.recoverAgentFailure(
                error = error,
                dslContext = dslContext,
                input = input,
                context = emptySet(),
                onFail = { null },
            )

            // Then
            assertThat(result).isNotNull
            assertThat(result!!.first).isEqualTo(conversation)
            assertThat(result.second).isTrue()
        }
    }

    @Test
    fun testRecoverAgentFailureWithRetrySignalException() {
        runBlocking {
            // Given
            val agent = mockk<ConversationAgent>()
            coEvery { agent.name } returns "TestAgent"
            val dslContext = mockk<DSLContext>()
            val input = "test input".toConversation()
            val retrySignal = RetrySignal(details = mapOf("key" to "value"))
            val resultConversation =
                Conversation(transcript = listOf(UserMessage("test"), AssistantMessage("retry result")))

            coEvery {
                agent.execute(any(), any())
            } returns Success(resultConversation)

            // When
            val result = agent.recoverAgentFailure(
                error = retrySignal,
                dslContext = dslContext,
                input = input,
                context = emptySet(),
                onFail = { null },
            )

            // Then
            assertThat(result).isNotNull
            assertThat(result!!.first).isEqualTo(resultConversation)
            assertThat(result.second).isTrue()

            coVerify {
                agent.execute(input, setOf(retrySignal))
            }
        }
    }

    @Test
    fun testRecoverAgentFailureWithRetrySignalAsCause() {
        runBlocking {
            // Given
            val agent = mockk<ConversationAgent>()
            coEvery { agent.name } returns "TestAgent"
            val dslContext = mockk<DSLContext>()
            val input = "test input".toConversation()
            val retrySignal = RetrySignal(details = mapOf("key" to "value"))
            val error = Exception("Wrapper exception", retrySignal)
            val resultConversation =
                Conversation(transcript = listOf(UserMessage("test"), AssistantMessage("retry result")))

            coEvery {
                agent.execute(any(), any())
            } returns Success(resultConversation)

            // When
            val result = agent.recoverAgentFailure(
                error = error,
                dslContext = dslContext,
                input = input,
                context = emptySet(),
                onFail = { null },
            )

            // Then
            assertThat(result).isNotNull
            assertThat(result!!.first).isEqualTo(resultConversation)
            assertThat(result.second).isTrue()

            coVerify {
                agent.execute(input, setOf(retrySignal))
            }
        }
    }

    @Test
    fun testRecoverAgentFailureWithOnFailFunction() {
        runBlocking {
            // Given
            val agent = mockk<ConversationAgent>()
            coEvery { agent.name } returns "TestAgent"
            val dslContext = mockk<DSLContext>()
            val input = "test input".toConversation()
            val error = Exception("Test error")
            val recoveryMessage = AssistantMessage("Recovery message")

            // When
            val result = agent.recoverAgentFailure(
                error = error,
                dslContext = dslContext,
                input = input,
                context = emptySet(),
                onFail = { recoveryMessage },
            )

            // Then
            assertThat(result).isNotNull
            assertThat(result!!.first.transcript).hasSize(input.transcript.size + 1)
            val lastMessage = result.first.transcript.last()
            assertThat(lastMessage).isInstanceOf(AssistantMessage::class.java)
            assertThat((lastMessage as AssistantMessage).content).isEqualTo(recoveryMessage.content)
            assertThat(result.second).isTrue()
        }
    }

    @Test
    fun testRecoverAgentFailureWhenOnFailThrowsWithConversationResult() {
        runBlocking {
            // Given
            val agent = mockk<ConversationAgent>()
            coEvery { agent.name } returns "TestAgent"
            val dslContext = mockk<DSLContext>()
            val input = "test input".toConversation()
            val error = Exception("Test error")
            val conversation =
                Conversation(transcript = listOf(UserMessage("test"), AssistantMessage("recovery response")))
            val onFailError = object : Exception(), WithConversationResult {
                override val conversation = conversation
            }

            // When
            val result = agent.recoverAgentFailure(
                error = error,
                dslContext = dslContext,
                input = input,
                context = emptySet(),
                onFail = { throw onFailError },
            )

            // Then
            assertThat(result).isNotNull
            assertThat(result!!.first).isEqualTo(conversation)
            assertThat(result.second).isTrue()
        }
    }

    @Test
    fun testRecoverAgentFailureWhenOnFailThrowsRetrySignal() {
        runBlocking {
            // Given
            val agent = mockk<ConversationAgent>()
            coEvery { agent.name } returns "TestAgent"
            val dslContext = mockk<DSLContext>()
            val input = "test input".toConversation()
            val error = Exception("Test error")
            val retrySignal = RetrySignal(details = mapOf("key" to "value"))
            val resultConversation =
                Conversation(transcript = listOf(UserMessage("test"), AssistantMessage("retry result")))

            coEvery {
                agent.execute(any(), any())
            } returns Success(resultConversation)

            // When
            val result = agent.recoverAgentFailure(
                error = error,
                dslContext = dslContext,
                input = input,
                context = emptySet(),
                onFail = { throw retrySignal },
            )

            // Then
            assertThat(result).isNotNull
            assertThat(result!!.first).isEqualTo(resultConversation)
            assertThat(result.second).isTrue()

            coVerify {
                agent.execute(input, setOf(retrySignal))
            }
        }
    }

    @Test
    fun testRecoverAgentFailureWhenOnFailThrowsAnotherException() {
        runBlocking {
            // Given
            val agent = mockk<ConversationAgent>()
            coEvery { agent.name } returns "TestAgent"
            val dslContext = mockk<DSLContext>()
            val input = "test input".toConversation()
            val error = Exception("Test error")
            val onFailError = Exception("OnFail error")

            // When
            val result = agent.recoverAgentFailure(
                error = error,
                dslContext = dslContext,
                input = input,
                context = emptySet(),
                onFail = { throw onFailError },
            )

            // Then
            assertThat(result).isNull()
        }
    }
}
