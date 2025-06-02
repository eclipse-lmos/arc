// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.openai.api.inbound

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAIRequest(
    val model: String,
    val messages: List<Message>,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    @SerialName("temperature") val temperature: Double? = null,
    @SerialName("top_p") val topP: Double? = null,
    @SerialName("n") val n: Int? = null,
    @SerialName("stream") val stream: Boolean? = null,
    @SerialName("stop") val stop: List<String>? = null,
)

@Serializable
data class ChatResponse(
    val id: String,
    @SerialName("object") val obj: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage,
    @SerialName("service_tier") val serviceTier: String? = null,
)

@Serializable
data class Choice(
    val index: Int,
    val message: Message,
    val logprobs: String? = null,
    @SerialName("finish_reason") val finishReason: String,
)

@Serializable
data class Message(
    val role: String,
    val content: String,
    val refusal: String? = null,
    val annotations: List<String> = emptyList(),
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens") val promptTokens: Int,
    @SerialName("completion_tokens") val completionTokens: Int,
    @SerialName("total_tokens") val totalTokens: Int,
    @SerialName("prompt_tokens_details") val promptTokensDetails: PromptTokensDetails,
    @SerialName("completion_tokens_details") val completionTokensDetails: CompletionTokensDetails,
)

@Serializable
data class PromptTokensDetails(
    @SerialName("cached_tokens") val cachedTokens: Int = 0,
    @SerialName("audio_tokens") val audioTokens: Int = 0,
)

@Serializable
data class CompletionTokensDetails(
    @SerialName("reasoning_tokens") val reasoningTokens: Int = 0,
    @SerialName("audio_tokens") val audioTokens: Int = 0,
    @SerialName("accepted_prediction_tokens") val acceptedPredictionTokens: Int = 0,
    @SerialName("rejected_prediction_tokens") val rejectedPredictionTokens: Int = 0,
)
