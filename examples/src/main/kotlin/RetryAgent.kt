// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.examples.retry

import kotlinx.coroutines.runBlocking
import org.eclipse.lmos.arc.agents.RetrySignal
import org.eclipse.lmos.arc.agents.agent.ask
import org.eclipse.lmos.arc.agents.agents
import org.eclipse.lmos.arc.agents.dsl.extensions.info
import org.eclipse.lmos.arc.agents.dsl.extensions.llm
import org.eclipse.lmos.arc.agents.dsl.getOptional
import org.eclipse.lmos.arc.agents.getChatAgent
import org.eclipse.lmos.arc.agents.retry
import org.eclipse.lmos.arc.core.getOrNull

/**
 * Demonstrates how to trigger an Agent to re-run.
 *
 * WorkFlow: The Agent is asked a question that it cannot answer. In the
 * filterOutput, an LLM is used to evaluate its response. If the response indicates that the question was not answered,
 * a retry is triggered with a feedback message.
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

    val agents = agents {
        agent {
            name = "MyAgent"
            model { "gpt-4o" }
            filterOutput {
                val eval = llm(
                    system = """
                Evaluate the following response and return NOT_ANSWERED if the response indicates that the question was not answered.
                """,
                    user = message,
                ).getOrNull()
                if (eval?.content?.contains("NOT_ANSWERED") == true) {
                    info("Current response: $message")
                    retry("Please say that the weather is sunny today.", max = 1)
                }
            }
            prompt {
                val retry = getOptional<RetrySignal>()
                """
                You are a helpful assistant. Help the user with their questions.
                Follow the following instructions: ${retry?.reason}
                """
            }
        }
    }

    val reply = agents.getChatAgent("MyAgent").ask("What is the weather like today?").getOrNull()
    println(reply)
}
