// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.examples.script

import kotlinx.coroutines.runBlocking
import org.eclipse.lmos.arc.scripting.hotReloadAgents
import org.eclipse.lmos.arc.server.ktor.serve
import java.io.File

/**
 * Demonstrates how run Agents that are defined in Kotlin script files.
 *
 * Changes to the script files will be automatically detected and reloaded.
 * New Agents can also be added to the folder without restarting the server.
 *
 * Setup:
 *  - Set the OpenAI API key as a System property or environment variable.
 *
 * Dependencies:
 *  - implementation(project(":arc-agents"))
 *  - implementation(project(":arc-azure-client"))
 *  - implementation(project(":arc-server"))
 *  - implementation(project(":arc-scripting"))
 */
fun main(): Unit = runBlocking {
    // Set the OpenAI API as a System property or environment variable.
    // System.setProperty("OPENAI_API_KEY", "****")

    hotReloadAgents(File("examples/agents")).serve(devMode = true)
}
