// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support

import org.eclipse.lmos.arc.agents.conversation.ConversationClassification

/**
 * A classification to indicate that the agent was unavailable to resolve the user's request.
 */
object Unresolved : ConversationClassification {
    override fun toString() = "UNRESOLVED"
}

/**
 * A classification to indicate that the agent has handed over the conversation to a human agent.
 * This can be because the user explicitly asked for a human agent or because the agent detected
 * that it cannot resolve the user's request.
 */
object AgentHandover : ConversationClassification {
    override fun toString() = "AGENT_HANDOVER"
}
