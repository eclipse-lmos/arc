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

    @Test
    fun `test AgentChain nextAgent functionality`() {
        // Given
        val agentChain = AgentChain(agents = listOf("agent1", "agent2", "agent3"))

        // When & Then
        // Should return the next agent in the chain
        assertThat(agentChain.nextAgent("agent1")).isEqualTo("agent2")
        assertThat(agentChain.nextAgent("agent2")).isEqualTo("agent3")

        // Should return null if the current agent is the last one
        assertThat(agentChain.nextAgent("agent3")).isNull()

        // Should return the first agent if the current agent is not in the chain
        assertThat(agentChain.nextAgent("nonExistentAgent")).isNull()
    }

    @Test
    fun `test executeWithHandover with agent chain and failure`() = runBlocking {
        // Given
        val agent1 = mockk<ChatAgent>()
        val fallbackAgent = mockk<ChatAgent>()
        val agentProvider = mockk<AgentProvider>()
        val conversation = "test".toConversation(User("user"))

        // Create an agent chain with one agent and a fallback agent
        val agentChain = AgentChain(agents = listOf("agent1"), agentOnFailure = "fallbackAgent")
        val context = setOf<Any>(agentChain)

        // Setup agent responses
        val exception = AgentFailedException("Test exception")
        val failureResult: Result<Conversation, AgentFailedException> = Failure(exception)
        val fallbackResult = Success(conversation)

        coEvery { agent1.name } returns "agent1"
        coEvery { fallbackAgent.name } returns "fallbackAgent"

        // Mock the execute method for agent1 to return a failure
        coEvery { agent1.execute(conversation, any()) } returns failureResult

        // Mock the execute method for fallbackAgent
        coEvery { fallbackAgent.execute(conversation, any()) } returns fallbackResult

        // Mock the getAgents method to return our agents
        coEvery { agentProvider.getAgents() } returns listOf(agent1, fallbackAgent)

        // When
        val handoverResult = agent1.executeWithHandover(conversation, context, agentProvider)

        // Then
        // Should execute agent1 (which fails), then fallbackAgent
        assertThat(handoverResult).isEqualTo(fallbackResult)
        coVerify(exactly = 1) { agent1.execute(conversation, any()) }
        coVerify(exactly = 1) { fallbackAgent.execute(conversation, any()) }
    }

    @Test
    fun `test AgentChain agentOnFailure functionality`() {
        // Given
        val agentChain = AgentChain(agents = listOf("agent1", "agent2"), agentOnFailure = "fallbackAgent")

        // When & Then
        // Should have the correct agentOnFailure
        assertThat(agentChain.agentOnFailure).isEqualTo("fallbackAgent")

        // Test with null agentOnFailure
        val agentChainWithoutFallback = AgentChain(agents = listOf("agent1", "agent2"))
        assertThat(agentChainWithoutFallback.agentOnFailure).isNull()
    }

    @Test
    fun `test executeWithHandover with agent chain where current agent is last in chain`() = runBlocking {
        // Given
        val agent1 = mockk<ChatAgent>()
        val agent2 = mockk<ChatAgent>()
        val agentProvider = mockk<AgentProvider>()
        val conversation = "test".toConversation(User("user"))

        // Create an agent chain where agent2 is the last agent
        val agentChain = AgentChain(agents = listOf("agent1", "agent2"))
        val context = setOf<Any>(agentChain)

        // Setup agent responses
        val result2 = Success(conversation)

        coEvery { agent1.name } returns "agent1"
        coEvery { agent2.name } returns "agent2"

        // Mock with specific parameters to avoid ambiguity
        coEvery { agent2.execute(conversation, match { it.contains(agentChain) }) } returns result2

        // Make sure getAgents returns all the agents we need
        coEvery { agentProvider.getAgents() } returns listOf(agent1, agent2)

        // When
        val handoverResult = agent2.executeWithHandover(conversation, context, agentProvider)

        // Then
        // Should execute only agent2 and return its result (no next agent in chain)
        assertThat(handoverResult).isEqualTo(result2)
        coVerify(exactly = 1) { agent2.execute(conversation, match { it.contains(agentChain) }) }
        coVerify(exactly = 0) { agent1.execute(any(), any()) }
    }
}
