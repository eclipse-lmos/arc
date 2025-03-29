// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.agent

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.AgentFailedException
import org.eclipse.lmos.arc.agents.AgentProvider
import org.eclipse.lmos.arc.agents.ChatAgent
import org.eclipse.lmos.arc.agents.TestBase
import org.eclipse.lmos.arc.agents.User
import org.eclipse.lmos.arc.agents.conversation.AIAgentHandover
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.toConversation
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.getOrThrow
import org.junit.jupiter.api.Test

class AgentHandoverHandlerTest : TestBase() {

    @Test
    fun `test executeWithHandover without handover`() = runBlocking {
        // Given
        val agent = mockk<ChatAgent>()
        val agentProvider = mockk<AgentProvider>()
        val conversation = "test".toConversation(User("user"))
        val context = setOf<Any>()
        val result = Success(conversation)

        coEvery { agent.execute(any(), any()) } returns result
        coEvery { agentProvider.getAgents() } returns emptyList()

        // When
        val handoverResult = agent.executeWithHandover(conversation, context, agentProvider)

        // Then
        assertThat(handoverResult).isEqualTo(result)
        coVerify(exactly = 1) { agent.execute(conversation, any()) }
    }

    @Test
    fun `test executeWithHandover with handover to another agent`() = runBlocking {
        // Given
        val agent = mockk<ChatAgent>()
        val nextAgent = mockk<ChatAgent>()
        val agentProvider = mockk<AgentProvider>()
        val conversation = "test".toConversation(User("user"))
        val context = setOf<Any>()

        // First agent returns a conversation with AIAgentHandover classification
        val handoverConversation = conversation.copy(classification = AIAgentHandover("nextAgent"))
        val firstResult = Success(handoverConversation)

        // Next agent returns a conversation without classification
        val finalConversation = handoverConversation.copy(classification = null)
        val finalResult = Success(finalConversation)

        coEvery { agent.name } returns "agent"
        coEvery { nextAgent.name } returns "nextAgent"
        coEvery { agent.execute(any(), any()) } returns firstResult
        coEvery { nextAgent.execute(any(), any()) } returns finalResult
        coEvery { agentProvider.getAgents() } returns listOf(agent, nextAgent)

        // When
        val handoverResult = agent.executeWithHandover(conversation, context, agentProvider)

        // Then
        assertThat(handoverResult).isEqualTo(finalResult)
        coVerify(exactly = 1) { agent.execute(conversation, any()) }
        coVerify(exactly = 1) { nextAgent.execute(handoverConversation.copy(classification = null), any()) }
    }

    @Test
    fun `test executeWithHandover with handover to same agent`() = runBlocking {
        // Given
        val agent = mockk<ChatAgent>()
        val agentProvider = mockk<AgentProvider>()
        val conversation = "test".toConversation(User("user"))
        val context = setOf<Any>()

        // First execution returns a conversation with AIAgentHandover classification to the same agent
        val handoverConversation = conversation.copy(classification = AIAgentHandover("agent"))
        val firstResult = Success(handoverConversation)

        // Second execution returns a conversation without classification
        val finalConversation = handoverConversation.copy(classification = null)
        val finalResult = Success(finalConversation)

        coEvery { agent.name } returns "agent"
        coEvery { agent.execute(conversation, any()) } returns firstResult
        coEvery { agent.execute(handoverConversation.copy(classification = null), any()) } returns finalResult
        coEvery { agentProvider.getAgents() } returns listOf(agent)

        // When
        val handoverResult = agent.executeWithHandover(conversation, context, agentProvider)

        // Then
        assertThat(handoverResult).isEqualTo(finalResult)
        coVerify(exactly = 1) { agent.execute(conversation, any()) }
        coVerify(exactly = 1) { agent.execute(handoverConversation.copy(classification = null), any()) }
    }

    @Test
    fun `test executeWithHandover with recursion limit reached`() = runBlocking {
        // Given
        val agent = mockk<ChatAgent>()
        val agentProvider = mockk<AgentProvider>()
        val conversation = "test".toConversation(User("user"))
        val context = setOf<Any>()

        // Set a low recursion limit
        val handoverLimit = AgentHandoverLimit(max = 2)

        // Create a conversation with AIAgentHandover classification
        val handoverConversation = conversation.copy(classification = AIAgentHandover("agent"))

        // Agent always returns a conversation with handover classification
        coEvery { agent.name } returns "agent"
        coEvery { agent.execute(any(), any()) } returns Success(handoverConversation)
        coEvery { agentProvider.getAgents() } returns listOf(agent)

        // When
        val handoverResult = agent.executeWithHandover(conversation, context + handoverLimit, agentProvider)

        // Then
        // After reaching the limit, it should return the last result without further recursion
        assertThat(handoverResult.getOrThrow().classification.toString()).isEqualTo(AIAgentHandover("agent").toString())

        // Verify that the agent was executed multiple times
        // The exact number might vary due to implementation details, but we should have at least 3 calls
        // (initial + recursions until limit)
        coVerify(atLeast = 3) { agent.execute(any(), any()) }
    }

    @Test
    fun `test executeWithHandover with agent not found`() = runBlocking {
        // Given
        val agent = mockk<ChatAgent>()
        val agentProvider = mockk<AgentProvider>()
        val conversation = "test".toConversation(User("user"))
        val context = setOf<Any>()

        // First agent returns a conversation with AIAgentHandover classification
        val handoverConversation = conversation.copy(classification = AIAgentHandover("nonExistentAgent"))
        val result = Success(handoverConversation)

        coEvery { agent.name } returns "agent"
        coEvery { agent.execute(any(), any()) } returns result
        coEvery { agentProvider.getAgents() } returns listOf(agent)

        // When
        val handoverResult = agent.executeWithHandover(conversation, context, agentProvider)

        // Then
        // Should return the original result if the next agent is not found
        assertThat(handoverResult).isEqualTo(result)
        coVerify(exactly = 1) { agent.execute(conversation, any()) }
    }

    @Test
    fun `test executeWithHandover with error in execution`() = runBlocking {
        // Given
        val agent = mockk<ChatAgent>()
        val agentProvider = mockk<AgentProvider>()
        val conversation = "test".toConversation(User("user"))
        val context = setOf<Any>()
        val exception = AgentFailedException("Test exception")
        val result: Result<Conversation, AgentFailedException> = Failure(exception)

        coEvery { agent.execute(any(), any()) } returns result
        coEvery { agentProvider.getAgents() } returns emptyList()

        // When
        val handoverResult = agent.executeWithHandover(conversation, context, agentProvider)

        // Then
        assertThat(handoverResult).isEqualTo(result)
        coVerify(exactly = 1) { agent.execute(conversation, any()) }
    }
}
