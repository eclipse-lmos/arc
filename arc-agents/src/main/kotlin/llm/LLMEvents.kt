// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.llm

import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.events.BaseEvent
import org.eclipse.lmos.arc.agents.events.Event
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.core.Result
import kotlin.time.Duration

/**
 * Events published by the LLM clients.
 */

data class LLMStartedEvent(val model: String) : Event by BaseEvent()

data class LLMFinishedEvent(
    val result: Result<AssistantMessage, ArcException>,
    val messages: List<ConversationMessage>,
    val functions: List<LLMFunction>?,
    val model: String,
    // TODO:: openAI SDK support Long For totalTokens, promptTokens, completionTokens. Revisit this
    val totalTokens: Int,
    val promptTokens: Int,
    val completionTokens: Int,
    val functionCallCount: Int,
    val duration: Duration,
    val settings: ChatCompletionSettings? = null,
) : Event by BaseEvent()
