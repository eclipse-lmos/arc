// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.examples.server

import kotlinx.coroutines.runBlocking
import org.eclipse.lmos.arc.agents.agents
import org.eclipse.lmos.arc.server.ktor.*

/**
 * Demonstrates how to run an Agent server.
 *
 * After starting the server, you can access the chat interface at:
 * http://localhost:8080/chat/index.html#/chat
 *
 * When devMode is disabled, the UI interface will not be loaded.
 *
 * Setup:
 *  - Set the OpenAI API key as a System property or environment variable.
 *
 * Dependencies:
 *  - implementation(project(":arc-agents"))
 *  - implementation(project(":arc-azure-client"))
 *  - implementation(project(":arc-server"))
 */
fun main(): Unit = runBlocking {
    // Set the OpenAI API as a System property or environment variable.
    // System.setProperty("OPENAI_API_KEY", "****")

    agents(
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
            prompt {
                """
                You are a weather assistant. Help the user with their questions about the weather.
                """
            }
        }
    }.serve(devMode = true)
}
