// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.events

import org.eclipse.lmos.arc.agents.events.BaseEvent
import org.eclipse.lmos.arc.agents.events.Event

/**
 * Emitted when a use case was triggered.
 */
data class UseCaseEvent(
    val name: String,
    val step: String? = null,
    val version: String? = null,
    val description: String? = null,
    val content: String? = null,
) : Event by BaseEvent()
