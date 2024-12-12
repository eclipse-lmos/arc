// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.filters

import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.dsl.AgentFilter
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.extensions.breakWith
import org.eclipse.lmos.arc.assistants.support.Unresolved

context(DSLContext)
class UnresolvedDetector(
    private val fallbackReply: DSLContext.() -> String,
) : AgentFilter {

    override suspend fun filter(message: ConversationMessage): ConversationMessage {
        if (message.content.contains("NO_ANSWER") || message.content.trim().isEmpty()) {
            breakWith(fallbackReply.invoke(this@DSLContext), classification = Unresolved)
        }
        if (message.content.contains(Unresolved.toString())) {
            breakWith(fallbackReply.invoke(this@DSLContext), classification = Unresolved)
        }
        return message
    }
}
