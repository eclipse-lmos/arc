// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.dsl.extensions

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.ChatAgent
import org.eclipse.lmos.arc.agents.TestBase
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.SECONDS
import kotlin.time.measureTime

class RateLimiterTest : TestBase() {

    @Test
    fun `test successful rate limiting`(): Unit = runBlocking {
        val agent = agent {
            name = "agent"
            limit { 1 / 3.seconds }
            prompt { "does stuff" }
        } as ChatAgent
        val time = measureTime {
            repeat(4) { executeAgent(agent, "question?") }
        }
        assertThat(time.toInt(SECONDS)).isGreaterThanOrEqualTo(9)
    }

    @Test
    fun `test successful rate limiting of multiple Agents`(): Unit = runBlocking {
        val agentA = agent {
            name = "agentA"
            limit { 1 / 3.seconds }
            prompt { "does stuff" }
        } as ChatAgent
        val agentB = agent {
            name = "agentB"
            limit { 1 / 5.seconds }
            prompt { "does stuff" }
        } as ChatAgent

        val time1 = measureTime {
            repeat(3) { executeAgent(agentA, "question?") }
        }
        assertThat(time1.toInt(SECONDS)).isGreaterThanOrEqualTo(6)

        val time2 = measureTime {
            repeat(3) { executeAgent(agentB, "question?") }
        }
        assertThat(time2.toInt(SECONDS)).isGreaterThanOrEqualTo(10)
    }

    @Test
    fun `test fallback`(): Unit = runBlocking {
        val agent = agent {
            name = "agentFallback"
            limit { 1 / 10.seconds withTimeout 0.seconds fallback { breakWith("try later") } }
            prompt { "does stuff" }
        } as ChatAgent
        executeAgent(agent, "question?")
        val result = executeAgent(agent, "question?")
        assertThat(result.second.transcript.last().content).isEqualTo("try later")
    }

    @Test
    fun `test rate limiter with model`(): Unit = runBlocking {
        val agent = agent {
            name = "agentModel"
            model {
                limit("model") { 1 / 10.seconds withTimeout 0.seconds } ?: "fallback"
            }
            prompt { "does stuff" }
        } as ChatAgent
        var model = "err"
        val provider = ChatCompleterProvider { name ->
            model = name ?: ""
            chatCompleter
        }
        executeAgent(agent, "question?", context = setOf(provider))
        assertThat(model).isEqualTo("model")

        executeAgent(agent, "question?", context = setOf(provider))
        assertThat(model).isEqualTo("fallback")
    }
}
