// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.langchain4j

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.model.output.Response
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
        response: Response<AiMessage>?,
        duration: Duration,
        functionCallHandler: FunctionCallHandler,
    ) {
        eventHandler?.publish(
            LLMFinishedEvent(
                result,
                messages,
                functions,
                config.modelName ?: settings?.deploymentNameOrModel() ?: "unknown",
                totalTokens = response?.tokenUsage()?.totalTokenCount() ?: -1,
                promptTokens = response?.tokenUsage()?.inputTokenCount() ?: -1,
                completionTokens = response?.tokenUsage()?.outputTokenCount() ?: -1,
                functionCallHandler.calledFunctions.size,
                duration,
                settings = settings,
            ),
        )
    }
}
