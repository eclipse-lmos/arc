// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.AgentFailedException
import org.eclipse.lmos.arc.agents.AgentProvider
import org.eclipse.lmos.arc.agents.ChatAgent
import org.eclipse.lmos.arc.agents.TestBase
import org.eclipse.lmos.arc.agents.conversation.AIAgentHandover
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.dsl.BasicDSLContext
import org.eclipse.lmos.arc.agents.dsl.OutputFilterContext
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.getOrNull
import org.junit.jupiter.api.Test

class AgentsTest : TestBase() {

    @Test
    fun `test nextAgent sets AIAgentHandover classification`(): Unit = runBlocking {
        // Create a conversation with a user message
        val input = Conversation(transcript = listOf(UserMessage("Hello")))
        val output = input.copy()

        // Create a DSLContext
        val dslContext = BasicDSLContext(testBeanProvider)

        // Create an OutputFilterContext with the input and output conversations
        val context = with(CoroutineScope(coroutineContext)) {
            OutputFilterContext(
                scriptingContext = dslContext,
                input = input,
                output = output,
                systemPrompt = "System prompt",
            )
        }

        // Call the nextAgent function with a test agent name
        context.nextAgent("testAgent")

        // Verify that the output conversation's classification is set to AIAgentHandover with the correct agent name
        assertThat(context.output.classification).isInstanceOf(AIAgentHandover::class.java)
        assertThat((context.output.classification as AIAgentHandover).name).isEqualTo("testAgent")
    }

    @Test
    fun `test callAgent with existing agent returns conversation`(): Unit = runBlocking {
        // Create a conversation with a user message
        val inputConversation = Conversation(transcript = listOf(UserMessage("Hello")))
        val outputConversation = Conversation(
            transcript = listOf(
                UserMessage("Hello"),
                AssistantMessage("Hi there!"),
            ),
        )

        // Mock ChatAgent
        val mockChatAgent = mockk<ChatAgent>()
        coEvery { mockChatAgent.execute(any(), any()) } returns Success(outputConversation)

        // Mock AgentProvider
        val mockAgentProvider = mockk<AgentProvider>()
        coEvery { mockAgentProvider.getAgents() } returns listOf(mockChatAgent)
        coEvery { mockChatAgent.name } returns "testAgent"

        // Set up the test environment
        testBeanProvider.setContext(setOf(mockAgentProvider, inputConversation)) {
            val dslContext = BasicDSLContext(testBeanProvider)

            // Call the callAgent function
            val result = dslContext.callAgent("testAgent", inputConversation)

            // Verify the result
            assertThat(result).isInstanceOf(Success::class.java)
            assertThat(result.getOrNull()).isEqualTo(outputConversation)
        }
    }

    @Test
    fun `test callAgent with non-existent agent throws exception`(): Unit = runBlocking {
        // Mock AgentProvider with no agents
        val mockAgentProvider = mockk<AgentProvider>()
        coEvery { mockAgentProvider.getAgents() } returns emptyList()

        // Create a conversation with a user message
        val inputConversation = Conversation(transcript = listOf(UserMessage("Hello")))

        // Set up the test environment
        testBeanProvider.setContext(setOf(mockAgentProvider, inputConversation)) {
            val dslContext = BasicDSLContext(testBeanProvider)

            // Call the callAgent function and expect an exception
            val result = dslContext.callAgent("nonExistentAgent", inputConversation)

            // Verify the result
            assertThat(result).isInstanceOf(Failure::class.java)
            val failure = result as Failure
            assertThat(failure.reason).isInstanceOf(AgentFailedException::class.java)
            assertThat(failure.reason.message).contains("Unknown agent 'nonExistentAgent'")
        }
    }

    @Test
    fun `test callAgent with non-ChatAgent throws exception`(): Unit = runBlocking {
        // Create a non-ChatAgent
        val mockAgent = mockk<org.eclipse.lmos.arc.agents.Agent<*, *>>()
        coEvery { mockAgent.name } returns "testAgent"

        // Mock AgentProvider
        val mockAgentProvider = mockk<AgentProvider>()
        coEvery { mockAgentProvider.getAgents() } returns listOf(mockAgent)

        // Create a conversation with a user message
        val inputConversation = Conversation(transcript = listOf(UserMessage("Hello")))

        // Set up the test environment
        testBeanProvider.setContext(setOf(mockAgentProvider, inputConversation)) {
            val dslContext = BasicDSLContext(testBeanProvider)

            // Call the callAgent function and expect an exception
            val result = dslContext.callAgent("testAgent", inputConversation)

            // Verify the result
            assertThat(result).isInstanceOf(Failure::class.java)
            val failure = result as Failure
            assertThat(failure.reason).isInstanceOf(AgentFailedException::class.java)
            assertThat(failure.reason.message).contains("Unknown agent 'testAgent'")
        }
    }

    @Test
    fun `test breakToAgent throws InterruptProcessingException with correct parameters`(): Unit = runBlocking {
        // Create a conversation with a user message
        val inputConversation = Conversation(transcript = listOf(UserMessage("Hello")))

        // Set up the test environment
        testBeanProvider.setContext(setOf(inputConversation)) {
            val dslContext = BasicDSLContext(testBeanProvider)

            // Call the breakToAgent function and expect an exception
            try {
                dslContext.breakToAgent("testAgent", reason = "Test reason")
                // If we get here, the test should fail because an exception should have been thrown
                assertThat(false).isTrue().withFailMessage("Expected InterruptProcessingException was not thrown")
            } catch (e: InterruptProcessingException) {
                // Verify the exception properties
                assertThat(e.conversation.classification).isInstanceOf(AIAgentHandover::class.java)
                assertThat((e.conversation.classification as AIAgentHandover).name).isEqualTo("testAgent")
                assertThat(e.message).contains("Test reason")
            }
        }
    }
}
