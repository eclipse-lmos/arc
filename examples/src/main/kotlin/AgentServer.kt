// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.examples

import kotlinx.coroutines.runBlocking
import org.eclipse.lmos.arc.agents.agents
import org.eclipse.lmos.arc.server.ktor.*

fun main() = runBlocking {
    // Only the api key is required.
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
    }.serve()
}
