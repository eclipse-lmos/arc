// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.conversation.toConversation
import org.eclipse.lmos.arc.agents.dsl.*
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.LLMFunctionException
import org.eclipse.lmos.arc.agents.functions.LLMFunctionProvider
import org.eclipse.lmos.arc.agents.functions.ParametersSchema
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.getOrThrow
import org.junit.jupiter.api.BeforeEach

open class TestBase {

    val testBeanProvider = CoroutineBeanProvider()
    val testAgentFactory = ChatAgentFactory(testBeanProvider)
    val testContext = BasicAgentDefinitionContext(testAgentFactory)
    val chatCompleter = mockk<ChatCompleter>()
    val chatCompleterProvider = ChatCompleterProvider { chatCompleter }
    val functionProvider: LLMFunctionProvider = mockk<LLMFunctionProvider>()
    val contextBeans =
        setOf(chatCompleterProvider, functionProvider, DateFilter(), NumberFilter())

    fun agent(agent: AgentDefinition.() -> Unit): Agent<*, *> {
        val context = BasicAgentDefinitionContext(testAgentFactory)
        context.agent(agent)
        return context.agents.first()
    }

    suspend fun executeAgent(
        agent: ChatAgent,
        message: String,
        llmResponse: String = "answer",
        context: Set<Any>? = null,
    ): Pair<List<ConversationMessage>, Conversation> {
        val input = slot<List<ConversationMessage>>()
        coEvery { chatCompleter.complete(capture(input)) } answers { Success(AssistantMessage(llmResponse)) }
        coEvery { chatCompleter.complete(capture(input), any()) } answers { Success(AssistantMessage(llmResponse)) }

        var result: Conversation
        testBeanProvider.setContext(context ?: contextBeans) {
            result = agent.execute(message.toConversation(User("user"))).getOrThrow()
        }
        return input.captured to result
    }

    @BeforeEach
    fun cleanMocks() {
        clearMocks(chatCompleter, functionProvider)
    }
}

class TestFunction(override val name: String = "test") : LLMFunction {
    override val parameters = ParametersSchema(emptyList(), emptyList())
    override val description: String = "test"
    override val group: String? = null
    override val isSensitive: Boolean = false
    override suspend fun execute(input: Map<String, Any?>): Result<String, LLMFunctionException> {
        return Success("test")
    }
}
