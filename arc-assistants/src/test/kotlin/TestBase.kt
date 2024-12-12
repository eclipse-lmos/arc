// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import org.eclipse.lmos.arc.agents.Agent
import org.eclipse.lmos.arc.agents.ChatAgent
import org.eclipse.lmos.arc.agents.User
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.conversation.toConversation
import org.eclipse.lmos.arc.agents.dsl.AgentDefinitionContext
import org.eclipse.lmos.arc.agents.dsl.BasicAgentDefinitionContext
import org.eclipse.lmos.arc.agents.dsl.ChatAgentFactory
import org.eclipse.lmos.arc.agents.dsl.SetBeanProvider
import org.eclipse.lmos.arc.agents.functions.LLMFunctionProvider
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.getOrThrow
import org.junit.jupiter.api.BeforeEach

open class TestBase {

    val chatCompleter = mockk<ChatCompleter>()
    val chatCompleterProvider = ChatCompleterProvider { chatCompleter }
    val functionProvider: LLMFunctionProvider = mockk<LLMFunctionProvider>()
    val testBeanProvider = SetBeanProvider(setOf(chatCompleterProvider, functionProvider))
    val testAgentFactory = ChatAgentFactory(testBeanProvider)

    fun assistantContext(fn: AgentDefinitionContext.() -> Unit): Agent<*, *> {
        val context = BasicAgentDefinitionContext(testAgentFactory)
        fn(context)
        return context.agents.first()
    }

    suspend fun executeAgent(
        agent: ChatAgent,
        message: String,
        llmResponse: String = "answer",
        context: Set<Any> = emptySet(),
    ): Pair<List<ConversationMessage>, Conversation> {
        val input = slot<List<ConversationMessage>>()
        coEvery { chatCompleter.complete(capture(input)) } answers { Success(AssistantMessage(llmResponse)) }
        coEvery { chatCompleter.complete(capture(input), any()) } answers { Success(AssistantMessage(llmResponse)) }

        val result = agent.execute(message.toConversation(User("user")), context).getOrThrow()
        return input.captured to result
    }

    @BeforeEach
    fun cleanMocks() {
        clearMocks(chatCompleter, functionProvider)
    }
}
