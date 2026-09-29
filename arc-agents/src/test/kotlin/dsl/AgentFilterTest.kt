// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl

import io.mockk.coEvery
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.AgentFailedException
import org.eclipse.lmos.arc.agents.ChatAgent
import org.eclipse.lmos.arc.agents.TestBase
import org.eclipse.lmos.arc.agents.User
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.conversation.SystemMessage
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.getOrThrow
import org.junit.jupiter.api.Test

class AgentFilterTest : TestBase() {

    @Test
    fun `test input filter - string replace`(): Unit = runBlocking {
        val agent = agent {
            name = ""
            description = ""
            systemPrompt = { "" }
            filterInput {
                "good" replaces "bad"
                "things" replaces "stuff"
            }
        }
        val (input, _) = executeAgent(agent as ChatAgent, "bad stuff")
        assertThat(input.last().content).isEqualTo("good things")
    }

    @Test
    fun `test output filter - string replace`(): Unit = runBlocking {
        val agent = agent {
            name = ""
            description = ""
            systemPrompt = { "" }
            filterOutput {
                "good" replaces "bad"
                "things" replaces "stuff"
            }
        }
        val (_, output) = executeAgent(agent as ChatAgent, "question", "bad stuff")
        assertThat(output.transcript.last().content).isEqualTo("good things")
    }

    @Test
    fun `test input filter - string remove using '-'`(): Unit = runBlocking {
        val agent = agent {
            name = ""
            description = ""
            systemPrompt = { "" }
            filterInput {
                -"bad"
                -"stuff"
            }
        }
        val (input, _) = executeAgent(agent as ChatAgent, "bad stuff here")
        assertThat(input.last().content).isEqualTo("  here")
    }

    @Test
    fun `test output filter - string remove using '-'`(): Unit = runBlocking {
        val agent = agent {
            name = ""
            description = ""
            systemPrompt = { "" }
            filterOutput {
                -"bad"
                -"stuff"
            }
        }
        val (_, output) = executeAgent(agent as ChatAgent, "question", "bad stuff here")
        assertThat(output.transcript.last().content).isEqualTo("  here")
    }

    @Test
    fun `test input filter - NumberFilter`(): Unit = runBlocking {
        val agent = agent {
            name = ""
            description = ""
            systemPrompt = { "" }
            filterInput {
                +NumberFilter()
            }
        }
        val (input, _) = executeAgent(agent as ChatAgent, "1234", "Bot")
        assertThat(input.last().content).isEqualTo("NUMBER")
    }

    @Test
    fun `test input filter - NumberFilter from context`(): Unit = runBlocking {
        val agent = agent {
            name = ""
            description = ""
            systemPrompt = { "" }
            filterInput {
                +NumberFilter::class
            }
        }
        val (input, _) = executeAgent(agent as ChatAgent, "1234", "Bot")
        assertThat(input.last().content).isEqualTo("NUMBER")
    }

    @Test
    fun `test input filter - Runs filters in parallel`(): Unit = runBlocking {
        val result = mutableListOf<String>()
        val agent = agent {
            name = ""
            description = ""
            systemPrompt = { "" }
            filterInput {
                runAsync {
                    delay(100)
                    result.add("2")
                }
                runAsync {
                    result.add("1")
                }
                result.add("0")
            }
        }
        val (_, _) = executeAgent(agent as ChatAgent, "hello", "1234")
        assertThat(result).isEqualTo(listOf("0", "1", "2"))
    }

    @Test
    fun `test output filter - NumberFilter`(): Unit = runBlocking {
        val agent = agent {
            name = ""
            description = ""
            systemPrompt = { "" }
            filterOutput {
                +NumberFilter()
            }
        }
        val (_, output) = executeAgent(agent as ChatAgent, "hello", "1234")
        assertThat(output.transcript.last().content).isEqualTo("NUMBER")
    }

    @Test
    fun `test output filter - NumberFilter from context`(): Unit = runBlocking {
        val agent = agent {
            name = ""
            description = ""
            systemPrompt = { "" }
            filterOutput {
                +NumberFilter::class
            }
        }
        val (_, output) = executeAgent(agent as ChatAgent, "hello", "1234")
        assertThat(output.transcript.last().content).isEqualTo("NUMBER")
    }

    @Test
    fun `test output filter - Runs filters in parallel`(): Unit = runBlocking {
        val result = mutableListOf<String>()
        val agent = agent {
            name = ""
            description = ""
            systemPrompt = { "" }
            filterOutput {
                runAsync {
                    delay(100)
                    result.add("2")
                }
                runAsync {
                    result.add("1")
                }
                result.add("0")
            }
        }
        val (_, _) = executeAgent(agent as ChatAgent, "hello", "1234")
        assertThat(result).isEqualTo(listOf("0", "1", "2"))
    }

    @Test
    fun `limitMessages retains last messages in order and keeps system prompt outside limit`(): Unit = runBlocking {
        val agent = agent {
            name = "limited"
            systemPrompt = { "generated prompt" }
            limitMessages { 2 }
        } as ChatAgent
        val history = conversation("old", "middle", "latest")

        val (request, result) = execute(agent, history)

        assertThat(request).containsExactly(
            SystemMessage("generated prompt"),
            UserMessage("middle", turnId = history.transcript[1].turnId),
            UserMessage("latest", turnId = history.transcript[2].turnId),
        )
        assertThat(result.transcript.map { it.content }).containsExactly("middle", "latest", "answer")
        assertThat(result.transcript.last()).isInstanceOf(AssistantMessage::class.java)
    }

    @Test
    fun `limitMessages boundary values retain expected history`(): Unit = runBlocking {
        val history = conversation("first", "second", "third")

        for ((limit, expected) in listOf(
            1 to listOf("third"),
            3 to listOf("first", "second", "third"),
            8 to listOf("first", "second", "third"),
        )) {
            val agent = agent {
                name = "limit-$limit"
                prompt { "prompt" }
                limitMessages { limit }
            } as ChatAgent
            val (request, result) = execute(agent, history)
            assertThat(request.drop(1).map { it.content }).containsExactlyElementsOf(expected)
            assertThat(result.transcript.map { it.content }).containsExactlyElementsOf(expected + "answer")
        }
    }

    @Test
    fun `limitMessages output filter receives filtered conversation`(): Unit = runBlocking {
        var outputFilterInput: List<String>? = null
        val agent = agent {
            name = "limited-output"
            prompt { "prompt" }
            limitMessages { 1 }
            filterOutput {
                outputFilterInput = input.transcript.map { it.content }
            }
        } as ChatAgent

        val (_, result) = execute(agent, conversation("old", "latest"))

        assertThat(outputFilterInput).containsExactly("latest")
        assertThat(result.transcript.map { it.content }).containsExactly("latest", "answer")
    }

    @Test
    fun `limitMessages is evaluated on each execution using DSL context`(): Unit = runBlocking {
        var evaluations = 0
        val agent = agent {
            name = "dynamic-limit"
            prompt { "prompt" }
            limitMessages {
                evaluations++
                context(Limit::class).value
            }
        } as ChatAgent
        val history = conversation("first", "second", "third")

        val (firstRequest, _) = execute(agent, history, context = setOf(Limit(1)))
        val (secondRequest, _) = execute(agent, history, context = setOf(Limit(2)))

        assertThat(firstRequest.drop(1).map { it.content }).containsExactly("third")
        assertThat(secondRequest.drop(1).map { it.content }).containsExactly("second", "third")
        assertThat(evaluations).isEqualTo(2)
    }

    @Test
    fun `limitMessages composes with input filters in declaration order`(): Unit = runBlocking {
        val limitedThenAppend = agent {
            name = "limited-then-append"
            prompt { "prompt" }
            limitMessages { 2 }
            filterInput { input = input.copy(transcript = input.transcript + UserMessage("added")) }
        } as ChatAgent
        val appendThenLimit = agent {
            name = "append-then-limited"
            prompt { "prompt" }
            filterInput { input = input.copy(transcript = input.transcript + UserMessage("added")) }
            limitMessages { 2 }
        } as ChatAgent
        val history = conversation("first", "second", "third")

        val (firstRequest, _) = execute(limitedThenAppend, history)
        val (secondRequest, _) = execute(appendThenLimit, history)

        assertThat(firstRequest.drop(1).map { it.content }).containsExactly("second", "third", "added")
        assertThat(secondRequest.drop(1).map { it.content }).containsExactly("third", "added")
    }

    @Test
    fun `limitMessages rejects zero and negative resolved limits`(): Unit = runBlocking {
        for (invalidLimit in listOf(0, -2)) {
            val agent = agent {
                name = "invalid-limit-$invalidLimit"
                prompt { "prompt" }
                limitMessages { context(Limit::class).value }
            } as ChatAgent
            lateinit var result: org.eclipse.lmos.arc.core.Result<Conversation, AgentFailedException>
            testBeanProvider.setContext(contextBeans) {
                result = agent.execute(conversation("old", "latest"), setOf(Limit(invalidLimit)))
            }

            assertThat(result).isInstanceOf(Failure::class.java)
            val failure = (result as Failure<*>).reason as AgentFailedException
            assertThat(failure.cause)
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("limitMessages")
                .hasMessageContaining("at least 1")
        }
    }

    @Test
    fun `agent without limitMessages keeps complete history`(): Unit = runBlocking {
        val agent = agent {
            name = "unlimited"
            prompt { "generated prompt" }
        } as ChatAgent
        val history = conversation("first", "second", "latest")

        val (request, result) = execute(agent, history)

        assertThat(request.map { it.content }).containsExactly("generated prompt", "first", "second", "latest")
        assertThat(result.transcript.map { it.content }).containsExactly("first", "second", "latest", "answer")
    }

    private data class Limit(val value: Int)

    private fun conversation(vararg messages: String) = Conversation(
        user = User("user"),
        transcript = messages.map { UserMessage(it) },
    )

    private suspend fun execute(
        agent: ChatAgent,
        input: Conversation,
        context: Set<Any> = emptySet(),
    ): Pair<List<ConversationMessage>, Conversation> {
        var sentMessages: List<ConversationMessage> = emptyList()
        coEvery { chatCompleter.complete(any(), any(), any(), any()) } answers {
            sentMessages = firstArg()
            Success(AssistantMessage("answer"))
        }
        lateinit var result: Conversation
        testBeanProvider.setContext(contextBeans) {
            result = agent.execute(input, context).getOrThrow()
        }
        return sentMessages to result
    }
}
