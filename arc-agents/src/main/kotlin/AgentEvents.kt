// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents

import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.events.BaseEvent
import org.eclipse.lmos.arc.agents.events.Event
import org.eclipse.lmos.arc.core.Result
import kotlin.time.Duration

/**
 * Collection of events that can be published by Agents.
 */
sealed class AgentEvent : Event by BaseEvent() {
    abstract val agent: Agent<*, *>
}

data class AgentFinishedEvent(
    override val agent: Agent<*, *>,
    val model: String?,
    val input: Conversation,
    val output: Result<Conversation, AgentFailedException>,
    val duration: Duration,
    val flowBreak: Boolean = false,
    val tools: Set<String> = emptySet(),
) : AgentEvent()

data class AgentStartedEvent(
    override val agent: Agent<*, *>,
) : AgentEvent()
