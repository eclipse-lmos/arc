// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.SystemMessage
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.get
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.core.result

/**
 * Extensions enabling accessing LLMs in the DSLContext.
 */
suspend fun DSLContext.llm(
    userMessage: String? = null,
    systemMessage: String? = null,
    model: String? = null,
    settings: ChatCompletionSettings? = null,
) = result<AssistantMessage, ArcException> {
    val chatCompleterProvider = get<ChatCompleterProvider>()
    val chatCompleter = chatCompleterProvider.provideByModel(model = model)
    val messages = buildList {
        if (systemMessage != null) add(SystemMessage(systemMessage))
        if (userMessage != null) add(UserMessage(userMessage))
    }
    return chatCompleter.complete(messages, null, settings = settings)
}
