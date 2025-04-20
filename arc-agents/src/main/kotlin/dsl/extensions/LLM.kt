// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.conversation.SystemMessage
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.dsl.AgentFilter
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.get
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.getOrNull
import org.eclipse.lmos.arc.core.result
import org.slf4j.LoggerFactory

/**
 * Extensions enabling accessing LLMs in the DSLContext.
 */
suspend fun DSLContext.llm(
    user: String? = null,
    system: String? = null,
    model: String? = null,
    settings: ChatCompletionSettings? = null,
) = result<AssistantMessage, ArcException> {
    val chatCompleterProvider = get<ChatCompleterProvider>()
    val chatCompleter = chatCompleterProvider.provideByModel(model = model)
    val messages = buildList {
        if (system != null) add(SystemMessage(system))
        if (user != null) add(UserMessage(user))
    }
    return chatCompleter.complete(messages, null, settings = settings)
}

/**
 * Transforms the input or output message using an LLM prompt.
 */
context(DSLContext)
class ApplyLLM(
    private val system: String,
    private val model: String? = null,
    private val settings: ChatCompletionSettings? = null,
    private val onError: ((Throwable) -> Unit)? = null,
) : AgentFilter {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun filter(message: ConversationMessage): ConversationMessage? {
        when (val result = llm(system = system, user = message.content, model = model, settings = settings)) {
            is Success -> {
                val assistantMessage = result.getOrNull() ?: return null
                return message.update(assistantMessage.content)
            }

            is Failure -> {
                log.error("LLM failed!", result.reason)
                onError?.invoke(result.reason)
                return message
            }
        }
    }
}
