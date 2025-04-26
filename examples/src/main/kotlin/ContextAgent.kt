// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.examples.context

import kotlinx.coroutines.runBlocking
import org.eclipse.lmos.arc.agents.agent.ask
import org.eclipse.lmos.arc.agents.agents
import org.eclipse.lmos.arc.agents.dsl.*
import org.eclipse.lmos.arc.agents.getChatAgent
import org.eclipse.lmos.arc.core.getOrNull

/**
 * Demonstrates how to provide Agents with access to external beans / components.
 *
 * This example shows how to use the get() function to access a bean in the context.
 *
 * Setup:
 *  - Set the OpenAI API key as a System property or environment variable.
 *
 * Dependencies:
 *  - implementation(project(":arc-agents"))
 *  - implementation(project(":arc-azure-client"))
 */
fun main(): Unit = runBlocking {
    // Set the OpenAI API as a System property or environment variable.
    // System.setProperty("OPENAI_API_KEY", "****")

    val agents = agents(context = setOf(CustomerData("Logan", 27))) {
        agent {
            name = "MyAgent"
            model { "gpt-4o" }
            prompt {
                val name = get<CustomerData>().name
                """
                    You are a helpful assistant. Help the user with their questions.
                    Always address the user as $name.
                """
            }
        }
    }

    val reply = agents.getChatAgent("MyAgent").ask("Hello").getOrNull()
    println(reply)
}

data class CustomerData(val name: String, val age: Int)
