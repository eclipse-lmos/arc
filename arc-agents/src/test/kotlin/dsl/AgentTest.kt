// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl

import io.mockk.coEvery
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.*
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.toConversation
import org.eclipse.lmos.arc.agents.events.BasicEventPublisher
import org.eclipse.lmos.arc.agents.events.EventHandler
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.ParametersSchema
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.getOrThrow
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class AgentTest : TestBase() {

    @Test
    fun `test agent meta data`(): Unit = runBlocking {
        val agent = agent {
            name = "name"
            description = "description"
            systemPrompt = { "systemPrompt" }
        }
        assertThat(agent.name).isEqualTo("name")
        assertThat(agent.description).isEqualTo("description")
    }

    @Test
    fun `test agent catches exception`(): Unit = runBlocking {
        val agent = agent {
            name = "name"
            description = "description"
            systemPrompt = { "systemPrompt" }
        } as ChatAgent
        coEvery { chatCompleter.complete(any(), any()) } answers { Failure(ArcException()) }

        val result: Result<Conversation, AgentFailedException>
        testBeanProvider.setContext(contextBeans) {
            result = agent.execute("question".toConversation(User("user")))
        }
        assertThat(result is Failure).isTrue()
        assertThat((result as Failure).reason.cause).isInstanceOf(ArcException::class.java)
    }

    @Test
    fun `test agent catches exception in filters`(): Unit = runBlocking {
        val agent = agent {
            name = "name"
            description = "description"
            systemPrompt = { "systemPrompt" }
            filterInput { error("test") }
        } as ChatAgent
        val result: Result<Conversation, AgentFailedException>
        testBeanProvider.setContext(contextBeans) {
            result = agent.execute("question".toConversation(User("user")))
        }
        assertThat(result is Failure).isTrue()
        assertThat((result as Failure).reason.cause).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `test agent tools`(): Unit = runBlocking {
        val agent = agent {
            name = "name"
            description = "description"
            systemPrompt = { "systemPrompt" }
            tools = listOf("myFunctions")
        }
        val functionGroup = slot<String>()
        coEvery { functionProvider.provide(capture(functionGroup)) } answers {
            Success(object : LLMFunction {
                override val name = "MyFunction"
                override val parameters = ParametersSchema(emptyList(), emptyList())
                override val description = "This is a sample function"
                override val group = "SampleGroup"
                override val isSensitive = false

                override suspend fun execute(input: Map<String, Any?>) = Success("execution result")
            })
        }

        executeAgent(agent as ChatAgent, "question")

        assertThat(functionGroup.captured).isEqualTo("myFunctions")
    }

    @Test
    fun `test agent tools function`(): Unit = runBlocking {
        val agent = agent {
            name = "name"
            description = "description"
            systemPrompt = { "systemPrompt" }
            tools { +"myDynamicFunctions" }
        }
        val functionGroup = slot<String>()
        coEvery { functionProvider.provide(capture(functionGroup)) } answers {
            Success(object : LLMFunction {
                override val name = "MyFunction"
                override val parameters = ParametersSchema(emptyList(), emptyList())
                override val description = "This is a sample function"
                override val group = "SampleGroup"
                override val isSensitive = false

                override suspend fun execute(input: Map<String, Any?>) = Success("execution result")
            })
        }

        executeAgent(agent as ChatAgent, "question")

        assertThat(functionGroup.captured).isEqualTo("myDynamicFunctions")
    }

    @Test
    fun `test agent publishes events`(): Unit = runBlocking {
        val agent = agent {
            name = "TestAgent"
            description = ""
            model = { "model" }
            systemPrompt = { "" }
        } as ChatAgent
        val eventPublisher = BasicEventPublisher()
        val agentEventHandler = AgentEventHandler()

        eventPublisher.add(agentEventHandler)
        executeAgent(agent, "question", context = contextBeans + eventPublisher)

        with(agentEventHandler.events[0] as AgentStartedEvent) {
            assertThat(agent.name).isEqualTo("TestAgent")
        }
        with(agentEventHandler.events[1] as AgentFinishedEvent) {
            assertThat(agent.name).isEqualTo("TestAgent")
            assertThat(model).isEqualTo("model")
            assertThat(input.transcript).hasSize(1)
            assertThat(input.transcript.first().content).isEqualTo("question")
            assertThat(output.getOrThrow().transcript).hasSize(2)
            assertThat(output.getOrThrow().transcript.last().content).isEqualTo("answer")
            assertThat(duration).isLessThan(1.seconds)
        }
    }
}

class AgentEventHandler : EventHandler<AgentEvent> {
    val events = mutableListOf<AgentEvent>()
    override fun onEvent(event: AgentEvent) {
        events.add(event)
    }
}
