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
    // Only the client name is required.
    System.setProperty("ARC_CLIENT", "ollama")

    val agents = agents {
        agent {
            name = "MyAgent"
            model { "gemma3:12b" }
            prompt {
                """
                You are a professional write. Help me to write a professional email.
                """
            }
        }
    }

    val reply = agents.getChatAgent("MyAgent").ask("Write me an email about bananas, please").getOrNull()
    println(reply)
}
