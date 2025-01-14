// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.scripting.agents

import org.eclipse.lmos.arc.agents.events.BaseEvent
import org.eclipse.lmos.arc.agents.events.Event

/**
 * Events published when an agent is loaded.
 */
data class AgentLoadedEvent(
    val name: String,
    val errorMessage: String? = null,
) : Event by BaseEvent()
