// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.examples

import kotlinx.coroutines.runBlocking
import org.eclipse.lmos.arc.agents.agent.ask
import org.eclipse.lmos.arc.agents.agents
import org.eclipse.lmos.arc.agents.getChatAgent
import org.eclipse.lmos.arc.core.getOrNull

fun main() = runBlocking {
    // Only the api key is required.
    // System.setProperty("OPENAI_API_KEY", "****")

    val agents = agents(
        functions = {
            function(
                name = "get_weather",
                description = "Get the weather for a given location.",
            ) {
                "THe weather is sunny"
            }
        },
    ) {
        agent {
            name = "MyAgent"
            model { "gpt-4o" }
            tools { +"get_weather" }
            prompt {
                """
                You are a weather assistant. Help the user with their questions about the weather.
                """
            }
        }
    }

    val reply = agents.getChatAgent("MyAgent").ask("What is the weather like today?").getOrNull()
    println(reply)
}
