// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.ChatAgent
import org.eclipse.lmos.arc.agents.TestBase
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
}
