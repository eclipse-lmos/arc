// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents

import io.mockk.coEvery
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.conversation.*
import org.eclipse.lmos.arc.agents.dsl.AllTools
import org.eclipse.lmos.arc.agents.dsl.extensions.breakWith
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.getOrThrow
import org.junit.jupiter.api.Test

class ChatAgentTest : TestBase() {

    @Test
    fun `test conversation is available in context`(): Unit = runBlocking {
        val agent = agent {
            name = "agent"
            description = "agent description"
            systemPrompt = { "does stuff" }
            filterInput {
                assertThat(input).isNotNull
            }
        } as ChatAgent
        val (_, result) = executeAgent(agent, "question?")
        assertThat(result.transcript.last().content).isEqualTo("answer")
    }

    @Test
    fun `test interrupting agent`(): Unit = runBlocking {
        val agent = agent {
            name = "agent"
            description = "agent description"
            systemPrompt = { "does stuff" }
            filterInput {
                breakWith("interrupted")
            }
        } as ChatAgent
        val result = agent.execute("test".toConversation(User("user")), contextBeans).getOrThrow()
        assertThat(result.transcript.last().content).isEqualTo("interrupted")
    }

    @Test
    fun `test setting message in OutputFilter`(): Unit = runBlocking {
        val agent = agent {
            name = "agent"
            description = "agent description"
            systemPrompt = { "does stuff" }
            filterOutput {
                message = "filterOutput"
            }
        } as ChatAgent
        val (_, result) = executeAgent(agent, "question?")
        assertThat(result.transcript.last().content).isEqualTo("filterOutput")
    }

    @Test
    fun `test setting message in InputFilter`(): Unit = runBlocking {
        val agent = agent {
            name = "agent"
            description = "agent description"
            systemPrompt = { "does stuff" }
            filterInput {
                message = "filterInput"
            }
        } as ChatAgent
        val (input, result) = executeAgent(agent, "question?")
        assertThat(result.transcript.last().content).isEqualTo("answer")
        assertThat(input.last().content).isEqualTo("filterInput")
    }

    @Test
    fun `test AllTools feature`(): Unit = runBlocking {
        val agent = agent {
            name = "agent"
            tools = AllTools
            systemPrompt = { "does stuff" }
        } as ChatAgent
        coEvery { functionProvider.provideAll() } answers { listOf(TestFunction("allToolsTest")) }

        executeAgent(agent, "question?")
        verify { functionProvider.provideAll() }
    }

    @Test
    fun `test init block execution`(): Unit = runBlocking {
        var initExecuted = 0
        val agent = agent {
            name = "agent"
            description = "agent description"
            systemPrompt = { "$initExecuted" }
            init = {
                initExecuted += 1
            }
        } as ChatAgent

        assertThat(initExecuted).isEqualTo(1)
        var result = testBeanProvider.setContext(setOf(ChatCompleterProvider { TestChatCompleter() })) {
            agent.execute("test".toConversation(User("user"))).getOrThrow()
        }
        assertThat(initExecuted.toString()).isEqualTo(result.transcript.findLast { it is AssistantMessage }?.content)

        result = testBeanProvider.setContext(setOf(ChatCompleterProvider { TestChatCompleter() })) {
            agent.execute("test".toConversation(User("user"))).getOrThrow()
        }
        assertThat(initExecuted.toString()).isEqualTo(result.transcript.findLast { it is AssistantMessage }?.content)
    }

    class TestChatCompleter : ChatCompleter {
        override suspend fun complete(
            messages: List<ConversationMessage>,
            functions: List<LLMFunction>?,
            settings: ChatCompletionSettings?,
        ): Result<AssistantMessage, ArcException> {
            return messages.findLast { it is SystemMessage }?.let {
                Success(AssistantMessage(it.content))
            } ?: throw ArcException("No system message found")
        }
    }
}
