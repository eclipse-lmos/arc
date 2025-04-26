// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.examples.ollama

import kotlinx.coroutines.runBlocking
import org.eclipse.lmos.arc.agents.agent.ask
import org.eclipse.lmos.arc.agents.agents
import org.eclipse.lmos.arc.agents.getChatAgent
import org.eclipse.lmos.arc.core.getOrNull

/**
 * Demonstrates how to connect your Agent to the Ollama server.
 *
 * Setup:
 *  - Start the Ollama server.
 *
 * Dependencies:
 *  implementation(project(":arc-agents"))
 *  implementation(project(":arc-langchain4j-client"))
 *  implementation("dev.langchain4j:langchain4j-ollama:0.36.2")
 */
fun main(): Unit = runBlocking {
    System.setProperty("ARC_CLIENT", "ollama")

    // Only needed if other clients are also configured.
    // For example, if the OpenAI key is set, this may load the OpenAI client.
    // In which case, you the framework needs to know which client to use for which model.
    // This is not needed if you only use the Ollama client.
    System.setProperty("ARC_MODEL", "llama3.2:latest")

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
