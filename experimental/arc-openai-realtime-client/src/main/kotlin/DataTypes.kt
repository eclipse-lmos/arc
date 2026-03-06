// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agent.client.ws

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.util.*

@Serializable
data class ResponseCreate(
    val response: ResponseData? = null,
    val type: String = "response.create",
)

@Serializable
data class ResponseData(
    val modalities: List<String>,
    val instructions: String,
)

@Serializable
data class ConversationItemCreate(
    val item: Item,
    val type: String = "conversation.item.create",
)

@Serializable
data class Item(
    val type: String,
    val role: String,
    val content: List<Content>,
)

@Serializable
data class Content(
    val type: String,
    val text: String? = null,
    val audio: String? = null,
)

fun textContent(text: String) = Content("text", text = text)

@Serializable
data class ResponseDone(
    @SerialName("event_id") val eventId: String,
    val type: String = "response.done",
    val response: Response,
)

@Serializable
data class Response(
    val id: String,
    val status: String,
    val output: List<Output>,
    val usage: Usage,
)

@Serializable
data class Output(
    val id: String,
    val type: String,
    val status: String,
    val role: String,
    val content: List<Content>,
)

@Serializable
data class Usage(
    @SerialName("total_tokens") val totalTokens: Int,
    @SerialName("input_tokens") val inputTokens: Int,
    @SerialName("output_tokens") val outputTokens: Int,
    @SerialName("input_token_details") val inputTokenDetails: InputTokenDetails,
    @SerialName("output_token_details") val outputTokenDetails: OutputTokenDetails,
)

@Serializable
data class InputTokenDetails(
    val cachedTokens: Int? = null,
    @SerialName("text_tokens") val textTokens: Int,
    @SerialName("audio_tokens") val audioTokens: Int,
    @SerialName("cached_tokens_details") val cachedTokensDetails: CachedTokensDetails,
)

@Serializable
data class CachedTokensDetails(
    @SerialName("text_tokens") val textTokens: Int,
    @SerialName("audio_tokens") val audioTokens: Int,
)

@Serializable
data class OutputTokenDetails(
    @SerialName("text_tokens") val textTokens: Int,
    @SerialName("audio_tokens") val audioTokens: Int,
)

@Serializable
data class AudioTranscriptDone(
    @SerialName("event_id") val eventId: String,
    @SerialName("type") val type: String,
    @SerialName("response_id") val responseId: String,
    @SerialName("item_id") val itemId: String,
    @SerialName("output_index") val outputIndex: Int,
    @SerialName("content_index") val contentIndex: Int,
    @SerialName("transcript") val transcript: String,
)

@Serializable
data class InputAudioBufferAppend(
    val audio: String,
    //  @SerialName("event_id") val eventId: String,
    val type: String = "input_audio_buffer.append",
)

@Serializable
data class InputAudioBufferCommit(
    //  @SerialName("event_id") val eventId: String,
    val type: String = "input_audio_buffer.commit",
)

@Serializable
data class SessionUpdate(
    val session: Session = Session(),
    @SerialName("event_id") val eventId: String = UUID.randomUUID().toString(),
    val type: String = "session.update",
)

@Serializable
data class Session(
    @SerialName("id") val id: String? = null,
    @SerialName("object") val objectType: String? = null,
    @SerialName("model") val model: String? = null,
    @SerialName("modalities") val modalities: List<String>? = null,
    @SerialName("instructions") val instructions: String? = null,
    @SerialName("voice") val voice: String? = null,
    @SerialName("input_audio_format") val inputAudioFormat: String? = null,
    @SerialName("output_audio_format") val outputAudioFormat: String? = null,
    @SerialName("input_audio_transcription") val inputAudioTranscription: InputAudioTranscription? = null,
    @SerialName("tool_choice") val toolChoice: String? = null,
    @SerialName("temperature") val temperature: Double? = null,
    @SerialName("max_response_output_tokens") val maxResponseOutputTokens: Int? = null,
    val tools: List<JsonObject>? = null,
)

@Serializable
data class InputAudioTranscription(
    val model: String = "whisper-1",
)

@Serializable
data class InputAudioTranscriptionCompleted(
    val transcript: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("item_id") val itemId: String,
    @SerialName("content_index") val contentIndex: Int,
    val type: String = "conversation.item.input_audio_transcription.completed",
)

@Serializable
data class ResponseAudioDelta(
    val delta: String,
    @SerialName("event_id") val eventId: String,
    val type: String = "response.audio.delta",
    @SerialName("response_id") val responseId: String,
    @SerialName("item_id") val itemId: String,
    @SerialName("output_index") val outputIndex: Int,
    @SerialName("content_index") val contentIndex: Int,
)
