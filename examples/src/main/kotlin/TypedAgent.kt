// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.examples.typed

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.agents.agents
import org.eclipse.lmos.arc.agents.getAgent
import org.eclipse.lmos.arc.core.getOrNull

/**
 * Demonstrates how to define the expected input and output types of an Agent.
 * The output type automatically enables JSON output and configures the output schema.
 *
 * Setup:
 *  - Set the OpenAI API key as a System property or environment variable.
 *
 * Dependencies:
 *  - implementation(project(":arc-agents"))
 *  - implementation(project(":arc-azure-client"))
 */
@Serializable
data class TravelRequest(
    val destination: String,
    val numberOfDays: Int,
)

@Serializable
data class TravelPlan(
    val destination: String,
    val activities: List<String>,
)

fun main(): Unit = runBlocking {
    // Set the OpenAI API key as a System property or environment variable.
    // System.setProperty("OPENAI_API_KEY", "****")

    val agents = agents {
        agent<TravelRequest, TravelPlan> {
            name = "TravelAgent"
            model { "gpt-4o" }
            prompt {
                """
                You are a travel assistant. Create a travel plan from the provided JSON request.
                """
            }
        }
    }

    val request = TravelRequest(destination = "Berlin", numberOfDays = 2)
    val reply = agents.getAgent<TravelRequest, TravelPlan>()
        .call(request)
        .getOrNull()

    println(reply)
}

