// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.client.langchain4j

import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.model.output.TokenUsage
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.tracing.Usage

fun TokenUsage.toUsage(): Usage {
    return Usage(
        promptCount = inputTokenCount(),
        completionCount = outputTokenCount(),
        totalCount = totalTokenCount(),
    )
}

fun List<ChatMessage>.toConversationMessages(): List<ConversationMessage> {
    // newline
    return map {
        when (it) {
            is dev.langchain4j.data.message.UserMessage -> org.eclipse.lmos.arc.agents.conversation.UserMessage(
                it.contents().joinToString(separator = "\n"),
            )

            is dev.langchain4j.data.message.AiMessage -> org.eclipse.lmos.arc.agents.conversation.AssistantMessage(
                it.text() ?: "",
            )

            is dev.langchain4j.data.message.SystemMessage -> org.eclipse.lmos.arc.agents.conversation.SystemMessage(
                it.text() ?: "",
            )

            is dev.langchain4j.data.message.ToolExecutionResultMessage -> org.eclipse.lmos.arc.agents.conversation.AssistantMessage(
                it.text() ?: "",
            )

            else -> error("Unsupported message type: ${it::class.simpleName}")
        }
    }
}
