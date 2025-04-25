// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.examples

import kotlinx.coroutines.runBlocking
import org.eclipse.lmos.arc.agents.agent.ask
import org.eclipse.lmos.arc.agents.agents
import org.eclipse.lmos.arc.agents.getChatAgent
import org.eclipse.lmos.arc.core.getOrNull

/**
 * Setup:
 *  - Start the Ollama server.
 */
fun main() = runBlocking {
    System.setProperty("ARC_CLIENT", "ollama")
    System.setProperty("ARC_MODEL", "llama3.2:latest") // Only needed if other client are also configured

    val agents = agents(
        functions = {
            function(name = "get_weather", description = "Returns the current weather.") {
                "the weather is sunny!"
            }
        },
    ) {
        agent {
            name = "MyAgent"
            model { "llama3.2:latest" }
            tools { +"get_weather" }
            prompt {
                """
                You are a weather assistant. Help the user with their questions about the weather.
                """
            }
        }
    }

    val reply = agents.getChatAgent("MyAgent").ask("How is the weather?").getOrNull()
    println(reply)
}
