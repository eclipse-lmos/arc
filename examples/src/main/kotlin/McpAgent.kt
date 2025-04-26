// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.examples.tools

import kotlinx.coroutines.runBlocking
import org.eclipse.lmos.arc.agents.agent.ask
import org.eclipse.lmos.arc.agents.agents
import org.eclipse.lmos.arc.agents.getChatAgent
import org.eclipse.lmos.arc.core.getOrNull

/**
 * Demonstrates how to connect your Agent to tools hosted on an MCP server.
 *
 * (make sure that the arc-mcp library is included in the classpath.)
 *
 * Setup:
 *  - Set the OpenAI API key as a System property or environment variable.
 *  - Start the McpApplication located in the test folder.
 *  - Set ARC_MCP_TOOLS_URLS variable to the URL of the MCP server.
 *
 * Dependencies:
 *  - implementation(project(":arc-agents"))
 *  - implementation(project(":arc-azure-client"))
 *  - implementation(project(":arc-mcp"))
 */
fun main(): Unit = runBlocking {
    // Set the OpenAI API as a System property or environment variable.
    // System.setProperty("OPENAI_API_KEY", "****")
    System.setProperty("ARC_MCP_TOOLS_URLS", "http://localhost:8080/")

    val agents = agents {
        agent {
            name = "MyBookAgent"
            model { "gpt-4o" }
            tools { +"getBooks" }
            prompt {
                """
                You are a helpful assistant. Help the user with their questions.
                """
            }
        }
    }

    agents.getChatAgent("MyBookAgent").ask("What books do you have today?").getOrNull()?.let {
        println(it)
    }
}
