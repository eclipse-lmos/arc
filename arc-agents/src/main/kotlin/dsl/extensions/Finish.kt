// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import org.eclipse.lmos.arc.agents.WithConversationResult
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.ConversationClassification
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.get

/**
 * Cancels the current execution of the Agent and returns the given message.
 */
suspend fun DSLContext.breakWith(
    message: String,
    classification: ConversationClassification? = null,
    reason: String? = null,
): Nothing {
    val conversationResult = get<Conversation>().copy(classification = classification) + AssistantMessage(message)
    throw InterruptProcessingException(conversationResult, reason)
}

class InterruptProcessingException(override val conversation: Conversation, reason: String?) :
    Exception("Agent processing interrupted. Reason:[${reason ?: "none"}]"), WithConversationResult

/**
 * Checks if the given exception or any of its causes is an InterruptProcessingException.
 * The InterruptProcessingException is not to be considered as an error, but as a control flow mechanism to
 * break the normal processing of the agent and return a specific message.
 */
fun Throwable.isFlowBreak(): Boolean {
    if (this is InterruptProcessingException) return true
    if (this.cause is InterruptProcessingException) return true
    if (this.cause?.cause is InterruptProcessingException) return true
    if (this.cause?.cause?.cause is InterruptProcessingException) return true
    return false
}
