// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.filters

import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.dsl.AgentOutputFilter
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.OutputFilterContext
import org.eclipse.lmos.arc.agents.dsl.extensions.breakWith
import org.eclipse.lmos.arc.assistants.support.Unresolved

class UnresolvedDetector(private val fallbackReply: DSLContext.() -> String) : AgentOutputFilter {

    override suspend fun filter(message: ConversationMessage, context: OutputFilterContext): ConversationMessage {
        with(context) {
            if (message.content.contains("NO_ANSWER") || message.content.trim().isEmpty()) {
                breakWith(fallbackReply.invoke(context), classification = Unresolved)
            }
            if (message.content.contains(Unresolved.toString())) {
                breakWith(fallbackReply.invoke(context), classification = Unresolved)
            }
        }
        return message
    }
}
