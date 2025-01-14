// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.spring.ai

import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.conversation.SystemMessage
import org.eclipse.lmos.arc.agents.conversation.UserMessage

/**
 * Converts a list of ConversationMessages to a list of Spring AI messages.
 */
fun List<ConversationMessage>.toSpringAI() = map { msg ->
    when (msg) {
        is UserMessage -> org.springframework.ai.chat.messages.UserMessage(msg.content)
        is AssistantMessage -> org.springframework.ai.chat.messages.AssistantMessage(msg.content)
        is SystemMessage -> org.springframework.ai.chat.messages.SystemMessage(msg.content)
    }
}
