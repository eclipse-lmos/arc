// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.openai

import com.openai.models.chat.completions.ChatCompletion
import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.llm.AIClientConfig
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.llm.LLMFinishedEvent
import org.eclipse.lmos.arc.core.Result
import kotlin.time.Duration

/**
 * Publishes LLM events
 */
class LLMEventPublisher(
    private val config: AIClientConfig,
    private val functions: List<LLMFunction>?,
    private val eventHandler: EventPublisher?,
    private val messages: List<ConversationMessage>,
    private val settings: ChatCompletionSettings?,
) {

    fun publishEvent(
        result: Result<AssistantMessage, ArcException>,
        chatCompletions: ChatCompletion?,
        duration: Duration,
    ) {
        val usage = chatCompletions?.usage()?.orElse(null)
        eventHandler?.publish(
            LLMFinishedEvent(
                result,
                messages,
                functions,
                config.modelName ?: settings?.deploymentNameOrModel() ?: "unknown",
                usage?.totalTokens()?.toInt() ?: -1,
                usage?.promptTokens()?.toInt() ?: -1,
                usage?.completionTokens()?.toInt() ?: -1,
                chatCompletions?.choices()?.getOrNull(0)?.message()?.toolCalls()?.orElse(null)?.size ?: 0,
                duration,
                settings = settings,
                finishReasons = chatCompletions?.choices()?.map { it.finishReason().toString() },
            ),
        )
    }
}
