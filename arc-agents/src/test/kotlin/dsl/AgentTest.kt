// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl

import io.mockk.coEvery
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.eclipse.lmos.arc.agents.*
import org.eclipse.lmos.arc.agents.agent.ask
import org.eclipse.lmos.arc.agents.agent.Skill
import org.eclipse.lmos.arc.agents.agent.SkillProvider
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.conversation.toConversation
import org.eclipse.lmos.arc.agents.events.BasicEventPublisher
import org.eclipse.lmos.arc.agents.events.EventHandler
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.ParametersSchema
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
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
            prompt { "systemPrompt" }
        } as ChatAgent
        coEvery { chatCompleter.complete(any(), any(), any()) } answers { Failure(ArcException()) }

        val result: Result<Conversation, AgentFailedException>
        testBeanProvider.setContext(contextBeans) {
            result = agent.execute("question".toConversation(User("user")))
        }
        assertThat(result is Failure).isTrue()
        assertThat((result as Failure).reason.cause).isInstanceOf(ArcException::class.java)
    }

    @Test
    fun `test onFail catch`(): Unit = runBlocking {
        val agent = agent {
            name = "name"
            onFail { AssistantMessage("Got it") }
            prompt { error("error") }
        } as ChatAgent

        val result: Result<String, AgentFailedException>
        testBeanProvider.setContext(contextBeans) {
            result = agent.ask("question")
        }
        assertThat(result is Success).isTrue()
        assertThat((result as Success).value).isEqualTo("Got it")
    }

    @Test
    fun `test onFail rethrow`(): Unit = runBlocking {
        val agent = agent {
            name = "name"
            onFail { null }
            prompt { error("error") }
        } as ChatAgent

        val result: Result<String, AgentFailedException>
        testBeanProvider.setContext(contextBeans) {
            result = agent.ask("question")
        }
        assertThat(result is Failure).isTrue()
        assertThat((result as Failure).reason.cause?.message).isEqualTo("error")
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
        coEvery { functionProvider.provide(capture(functionGroup), any()) } answers {
            Success(object : LLMFunction {
                override val name = "MyFunction"
                override val version = "1.0"
                override val parameters = ParametersSchema()
                override val description = "This is a sample function"
                override val group = "SampleGroup"
                override val isSensitive = false
                override val outputDescription = "This is the output description"

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
        coEvery { functionProvider.provide(capture(functionGroup), any()) } answers {
            Success(object : LLMFunction {
                override val name = "MyFunction"
                override val version = "1.0"
                override val parameters = ParametersSchema()
                override val description = "This is a sample function"
                override val group = "SampleGroup"
                override val isSensitive = false
                override val outputDescription = "This is the output description"

                override suspend fun execute(input: Map<String, Any?>) = Success("execution result")
            })
        }

        executeAgent(agent as ChatAgent, "question")

        assertThat(functionGroup.captured).isEqualTo("myDynamicFunctions")
    }

    @Test
    fun `test agent skills inject metadata and provide an activation tool`(): Unit = runBlocking {
        var systemPrompt = ""
        var loadedFunctions: List<LLMFunction>? = null
        val completer = object : ChatCompleter {
            override suspend fun complete(
                messages: List<ConversationMessage>,
                functions: List<LLMFunction>?,
                settings: ChatCompletionSettings?,
                eventPublisher: org.eclipse.lmos.arc.agents.events.EventPublisher?,
            ): Result<AssistantMessage, ArcException> {
                systemPrompt = messages.first().content
                loadedFunctions = functions
                return Success(AssistantMessage("answer"))
            }
        }
        val agent = agent {
            name = "name"
            skills { +"writing" }
            prompt { SKILLS }
        } as ChatAgent

        testBeanProvider.setContext(
            setOf(
                ChatCompleterProvider { completer },
                SkillProvider { name ->
                    if (name == "writing") {
                        """
                        ---
                        name: writing
                        description: Writes concise answers.
                        ---

                        Write concise answers.
                        """.trimIndent()
                    } else null
                },
            ),
        ) {
            agent.execute("question".toConversation(User("user"))).getOrThrow()
        }

        assertThat(systemPrompt).isEqualTo("Available skills:\n- name: writing\n  description: Writes concise answers.")
        val skillTool = loadedFunctions.orEmpty().single { it.name == "activate_skill" }
        assertThat(skillTool.execute(mapOf("name" to "writing")).getOrThrow()).isEqualTo("Write concise answers.")
        val unknownSkill = skillTool.execute(mapOf("name" to "unknown"))
        assertThatThrownBy { unknownSkill.getOrThrow() }
            .hasMessageContaining("Could not load skill")
    }

    @Test
    fun `test agent retains explicit skill metadata`(): Unit = runBlocking {
        val skill = Skill(id = "writing", name = "Writing", description = "Writes answers")
        val agent = agent {
            name = "name"
            skills { listOf(skill) }
            prompt { "system" }
        }

        assertThat(agent.fetchSkills()).containsExactly(skill)
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
