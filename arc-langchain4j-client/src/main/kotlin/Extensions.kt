// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.client.langchain4j

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.TextContent
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.output.TokenUsage
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage

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

fun ChatMessage.safeText(): String? =
    when (this) {
        is UserMessage -> {
            if (this.hasSingleText()) {
                this.singleText()
            } else {
                this.contents()
                    .filterIsInstance<TextContent>()
                    .map { it.text() }
                    .filter { it.isNotBlank() }
                    .joinToString(separator = "")
                    .ifEmpty { null }
            }
        }
        is AiMessage -> {
            val t = this.text()
            if (t.isNullOrBlank()) null else t
        }
        is SystemMessage -> {
            val t = this.text()
            if (t.isNullOrBlank()) null else t
        }
        is ToolExecutionResultMessage -> {
            val t = this.text()
            if (t.isNullOrBlank()) null else t
        }
        else -> null
    }

fun UserMessage.extractText(): String? {
    // If the message is a single text, use singleText() (safe)
    return when {
        this.hasSingleText() -> this.singleText()
        else -> {
            // Otherwise try to collect all TextContent pieces and join them
            this.contents()
                .filterIsInstance<TextContent>()
                .map { it.text() }
                .filter { it.isNotEmpty() }
                .joinToString(separator = "")
                .ifEmpty { null }
        }
    }
}
