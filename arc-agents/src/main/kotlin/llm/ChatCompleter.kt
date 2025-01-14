// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.llm

import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.core.Result

/**
 * ChatCompleters can take a conversation and adds a new message to the conversation.
 * They are usually connect to a LLM Model, such as, ChatGPT or Gemini.
 */
interface ChatCompleter {
    suspend fun complete(
        messages: List<ConversationMessage>,
        functions: List<LLMFunction>? = null,
        settings: ChatCompletionSettings? = null,
    ): Result<AssistantMessage, ArcException>
}

suspend fun ChatCompleter.complete(
    userMessage: String,
    functions: List<LLMFunction>? = null,
    settings: ChatCompletionSettings? = null,
) = complete(listOf(UserMessage(userMessage)), functions, settings)
