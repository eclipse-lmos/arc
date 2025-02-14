// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agent.client.ws

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.eclipse.lmos.arc.agent.client.ws.sound.pcm16ToWav
import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.conversation.DataStream
import org.eclipse.lmos.arc.agents.conversation.SystemMessage
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.conversation.WritableDataStream
import org.eclipse.lmos.arc.agents.conversation.asDataStream
import org.eclipse.lmos.arc.agents.conversation.readAllBytes
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.core.result
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration.Companion.seconds

/**
 * Implementation of [ChatCompleter] that uses OpenAI's Realtime API to communicate with the agents.
 */
class OpenAIRealtimeClient(private val url: String, private val key: String) : Closeable, ChatCompleter {

    private val log = LoggerFactory.getLogger(this.javaClass)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 20.seconds
        }
    }

    override suspend fun complete(
        messages: List<ConversationMessage>,
        functions: List<LLMFunction>?,
        settings: ChatCompletionSettings?,
    ) = result<AssistantMessage, ArcException> {
        val result = AtomicReference<AssistantMessage>()
        client.webSocket(url, {
            header("Authorization", "Bearer $key")
            header("OpenAI-Beta", "realtime=v1")
        }) {
            log.debug("Connected to $url")
            val (text, audio, userTranscript) = consumeEvents(messages, functions, settings)

            result.set(
                AssistantMessage(
                    text,
                    userTranscript = userTranscript,
                    binaryData = listOf(wavData(pcm16ToWav(audio.readAllBytes()).asDataStream())),
                ),
            )
            close()
        }
        result.get()
    }

    /**
     * Consumes incoming events from the OpenAI Server and send data to the server.
     */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun DefaultClientWebSocketSession.consumeEvents(
        messages: List<ConversationMessage>,
        functions: List<LLMFunction>?,
        settings: ChatCompletionSettings?,
    ): Triple<String, DataStream, String> {
        val response = AtomicReference("")
        val userTranscript = AtomicReference("")
        val audioData = WritableDataStream()
        var binaryStreamJob: Job? = null

        incoming.consumeEach { data ->
            val dataText = (data as Frame.Text).readText()
            val dataJson = json.parseToJsonElement(dataText)
            val type = dataJson.jsonObject["type"]?.jsonPrimitive?.content
            log.debug("Received event with type: $type")

            when (type) {
                "session.created" -> {
                    send(
                        SessionUpdate(
                            Session(
                                temperature = settings?.temperature,
                                maxResponseOutputTokens = settings?.maxTokens,
                                inputAudioTranscription = InputAudioTranscription(),
                                instructions = messages.firstOrNull { it is SystemMessage }?.content,
                                tools = functions?.map { it.toJsonSchema() },
                            ),
                        ),
                    )
                }

                "session.updated" -> {
                    log.debug("Session updated: $dataText")
                    sendHistory(messages.dropLast(1))
                    binaryStreamJob = async { sendBinaries(messages) }
                    // Not required with VAD active
                    // send(InputAudioBufferCommit())
                    // send(ResponseCreate())
                }

                "response.audio.delta" -> {
                    val event = json.decodeFromJsonElement<ResponseAudioDelta>(dataJson)
                    audioData.write(Base64.decode(event.delta))
                }

                "input_audio_buffer.speech_stopped" -> {
                    // binaryStreamJob?.cancel()
                }

                "response.audio_transcript.done" -> {
                    val event = json.decodeFromJsonElement<AudioTranscriptDone>(dataJson)
                    response.set(event.transcript)
                }

                "conversation.item.input_audio_transcription.completed" -> {
                    val event = json.decodeFromJsonElement<InputAudioTranscriptionCompleted>(dataJson)
                    log.info("User Transcript ${event.transcript}")
                    userTranscript.set(event.transcript)
                    if (response.get().isNotBlank()) {
                        binaryStreamJob?.cancel()
                        audioData.close()
                        return Triple(response.get(), audioData, userTranscript.get())
                    }
                }

                "error" -> {
                    log.error("Received error from OpenAI: $dataText")
                    throw ArcException(dataText)
                }

                "response.done" -> {
                    if (userTranscript.get().isNotBlank()) {
                        binaryStreamJob?.cancel()
                        audioData.close()
                        return Triple(response.get(), audioData, userTranscript.get())
                    }
                }

                else -> {
                    log.debug(dataText)
                }
            }
        }
        return Triple(response.get(), audioData, userTranscript.get())
    }

    /**
     * Sends the binary data of the messages to the server.
     */
    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun DefaultClientWebSocketSession.sendBinaries(messages: List<ConversationMessage>) {
        messages.lastOrNull()?.binaryData?.lastOrNull()?.stream?.stream()?.collect { chunk ->
            send(InputAudioBufferAppend(audio = Base64.encode(chunk)))
            log.info("Sent audio chunk ${chunk.size}")
        }
    }

    /**
     * Creates the history of messages in the OpenAI conversation.
     */
    private suspend fun DefaultClientWebSocketSession.sendHistory(messages: List<ConversationMessage>) {
        messages
            .filter { it !is SystemMessage }
            .filter { it.content.isNotBlank() }
            .forEach {
                log.debug("Updating history with: ${it.content}")
                send(
                    ConversationItemCreate(
                        item = Item(
                            type = "message",
                            role = when (it) {
                                is UserMessage -> "user"
                                else -> "assistant"
                            },
                            content = listOf(textContent(it.content)),
                        ),
                    ),
                )
            }
    }

    /**
     * Converts a [ConversationMessage]s to OpenAI Conversation Items.
     */
    private fun ConversationMessage.toConversationItem() = when (this) {
        is UserMessage -> Item(
            type = "message",
            role = "user",
            content = listOf(textContent(content)),
        )

        is AssistantMessage -> Item(
            type = "message",
            role = "assistant",
            content = listOf(textContent(content)),
        )

        else -> null
    }

    private suspend inline fun <reified T> WebSocketSession.send(msg: T) {
        send(Frame.Text(json.encodeToString(msg)))
    }

    /**
     * Closes the client and websocket connection.
     */
    override fun close() {
        client.close()
    }
}
