// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DSLAgentsTest : TestBase() {

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
    fun `test loading functions`() {
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
}
