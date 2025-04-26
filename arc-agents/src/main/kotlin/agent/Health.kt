// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.agent

import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.agents.ArcAgents
import org.eclipse.lmos.arc.agents.dsl.provideOptional
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.memory.Memory
import org.eclipse.lmos.arc.core.*

/**
 * Represents the health status of the ARC system.
 *
 * @property ok Indicates whether the system is functioning properly.
 * @property chatCompletersAvailable Indicates whether chat completers are available for use.
 * @property memory String representation of the memory component's status.
 * @property chatCompleterProvider String representation of the chat completer provider.
 */
@Serializable
class Health(val ok: Boolean, val chatCompletersAvailable: Boolean, val memory: String, val chatCompleterProvider: String)

/**
 * Extension function for ArcAgents that checks and returns the health status of the system.
 *
 * This function verifies the availability of chat completers and collects information
 * about the memory component and chat completer provider.
 *
 * @return A Health object containing the current health status of the system.
 */
suspend fun ArcAgents.health(): Health {
    val chatCompleterProvider = provide(ChatCompleterProvider::class)
    val memory = provideOptional<Memory>()
    val chatCompleter = result<ChatCompleter, Exception> {
        chatCompleterProvider.provideByModel(null)
    }.getOrNull()
    return Health(
        ok = chatCompleter != null,
        chatCompletersAvailable = true,
        chatCompleterProvider = chatCompleterProvider.toString(),
        memory = memory.toString(),
    )
}
