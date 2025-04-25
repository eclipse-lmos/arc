// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.examples

import kotlinx.coroutines.runBlocking
import org.eclipse.lmos.arc.scripting.hotReloadAgents
import org.eclipse.lmos.arc.server.ktor.serve
import java.io.File

fun main() = runBlocking {
    // Only the api key is required.
    // System.setProperty("OPENAI_API_KEY", "****")

    hotReloadAgents(File("examples/agents")).serve(devMode = true)
}
