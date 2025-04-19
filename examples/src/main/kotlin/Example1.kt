// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.examples

import org.eclipse.lmos.arc.agents.agents

fun main() {
    System.setProperty("OPENAI_API_KEY", "your_api_key_here")

    val agents = agents({
        function(name = "get_weather", description = "Get the weather") {
            """The weather is sunny. A lovely 32 degrees celsius."""
        }
    }) {
        agent {
            name = "AzureAgent"
            prompt {
                """
                You are a professional weather service. You provide weather data to your users.
                """
            }
        }
    }

    println(agents.getAgents())
}
