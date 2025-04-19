// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.examples

import kotlinx.coroutines.runBlocking
import org.eclipse.lmos.arc.agents.agent.ask
import org.eclipse.lmos.arc.agents.agents
import org.eclipse.lmos.arc.agents.dsl.AllTools
import org.eclipse.lmos.arc.agents.getChatAgent
import org.eclipse.lmos.arc.core.getOrNull

fun main() = runBlocking {
    val agents = agents(functions = {
        function(name = "get_weather", description = "Get the weather") {
            """The weather is sunny. A lovely 32 degrees celsius."""
        }
    }) {
        agent {
            name = "MyAgent"
            tools = AllTools
            prompt {
                """
                You are a professional weather service. You provide weather data to your users.
                """
            }
        }
    }

    val reply = agents.getChatAgent("MyAgent").ask("What is the weather?").getOrNull()
    println(reply)
}
