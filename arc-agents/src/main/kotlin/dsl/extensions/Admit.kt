// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.dsl.AgentInputFilter
import org.eclipse.lmos.arc.agents.dsl.InputFilterContext
import org.eclipse.lmos.arc.agents.dsl.get
import org.slf4j.LoggerFactory

/**
 * A simple function that can be used to admit a certain percentage of conversations while rejecting the rest with a
 * rejection message.
 *
 * The implementation uses the hash of the conversation id to determine if the conversation should be rejected.
 * This is helpful as it means that conversations spread between multiple instances of the agent will have the same
 * reject status.
 *
 * The downside is that it relies on the conversation id having a good hash
 * distribution for the percentage value to be accurate.
 */
suspend fun InputFilterContext.admit(percent: Int, returnValue: String) {
    +AdmitFilter(percent, returnValue)
}

class AdmitFilter(
    private val percent: Int,
    private val rejectValue: String,
) :
    AgentInputFilter {

    init {
        require(percent in 1..100) { "Percent must be between 1 and 100" }
    }

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun filter(message: ConversationMessage, context: InputFilterContext): ConversationMessage {
        val conversationId = context.get<Conversation>().conversationId
        val hashBucket = conversationId.hashCode() % 100
        val accepted = hashBucket <= percent
        if (!accepted) {
            log.debug("Rejecting message: $conversationId")
            context.breakWith(rejectValue, reason = "AdmitFilter: rejected")
        }
        return message
    }
}
