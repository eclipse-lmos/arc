// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents

import io.mockk.coEvery
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.llm.OutputFormat
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.getOrNull
import org.junit.jupiter.api.Test

class DSLAgentsTest : TestBase() {

    @Serializable
    data class MyInput(val question: String)

    @Serializable
    data class MyOutput(val answer: String)

    @Test
    fun `test loading agents`() {
        val agentBuilder = DSLAgents.init(chatCompleterProvider)
        agentBuilder.define {
            agent {
                name = "agent"
                description = "agent description"
                systemPrompt = { "does stuff" }
            }
        }
        val result = agentBuilder.getAgents()
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("agent")
    }

    @Test
    fun `test loading functions`(): Unit = runBlocking {
        val agentBuilder = DSLAgents.init(chatCompleterProvider)
        agentBuilder.defineFunctions {
            function(
                name = "get_weather",
                description = "the weather service",
                params = types(string("location", "the location")),
            ) {
                "result"
            }
        }
        val result = agentBuilder.provideAll()
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("get_weather")
    }

    @Test
    fun `test typed agent sets json output schema`(): Unit = runBlocking {
        val agentBuilder = DSLAgents.init(chatCompleterProvider)
        agentBuilder.define {
            agent<MyInput, MyOutput> {
                name = "agent"
                prompt { "does stuff" }
            }
        }

        val settings = slot<ChatCompletionSettings?>()
        coEvery {
            chatCompleter.complete(any(), any(), captureNullable(settings), eventPublisher = any())
        } returns Success(AssistantMessage("""{"answer":"answer"}"""))

        val output = agentBuilder.getAgent<MyInput, MyOutput>()
            .call(MyInput("question?"))
            .getOrNull()

        assertThat(output).isEqualTo(MyOutput("answer"))
        assertThat(settings.captured).isNotNull
        assertThat(settings.captured?.format).isEqualTo(OutputFormat.JSON)
        assertThat(settings.captured?.outputSchema?.type).isEqualTo(MyOutput::class)
    }

    @Test
    fun `test typed agent name disambiguates matching type pairs`() {
        val agentBuilder = DSLAgents.init(chatCompleterProvider).define {
            agent<MyInput, MyOutput> { name = "first" }
            agent<MyInput, MyOutput> { name = "second" }
        }

        assertThatThrownBy { agentBuilder.getAgent<MyInput, MyOutput>() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Multiple conversation agents")
            .hasMessageContaining("specify an agent name")
        assertThat(agentBuilder.getAgent<MyInput, MyOutput>("first")).isNotNull
    }
}
