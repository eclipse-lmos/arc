// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.ollama

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import kotlinx.serialization.json.*
import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.conversation.*
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.llm.*
import org.eclipse.lmos.arc.agents.llm.OutputFormat.JSON
import org.eclipse.lmos.arc.core.*
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * Calls a Ollama Chat endpoint to complete a conversation.
 */
@Deprecated(
    "Use the Arc Langchain4j Ollama client instead",
)
class OllamaClient(
    private val languageModel: OllamaClientConfig,
    private val eventHandler: EventPublisher? = null,
) : ChatCompleter, TextEmbedder {

    private val log = LoggerFactory.getLogger(javaClass)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val client = HttpClient(CIO) {
        install(DefaultRequest) {
            url(languageModel.url ?: "http://localhost:11434")
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    log.debug(message)
                }
            }
            level = LogLevel.ALL
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    override suspend fun complete(
        messages: List<ConversationMessage>,
        functions: List<LLMFunction>?,
        settings: ChatCompletionSettings?,
    ) = result<AssistantMessage, ArcException> {
        val ollamaMessages = toOllamaMessages(messages)

        val ollamaTools = languageModel.toolSupported.takeIf { it }?.let { toOllamaTools(functions) } ?: emptyList()

        val functionCallHandler = FunctionCallHandler(functions ?: emptyList(), eventHandler)

        eventHandler?.publish(LLMStartedEvent(languageModel.modelName))
        val result: Result<ChatResponse, ArcException>
        val duration = measureTime {
            result = chat(ollamaMessages, ollamaTools, functionCallHandler, settings)
        }
        var chatResponse: ChatResponse? = null
        finally { publishEvent(it, messages, functions, chatResponse, duration, functionCallHandler, settings) }
        chatResponse = result failWith { it }
        chatResponse.getFirstAssistantMessage(
            sensitive = functionCallHandler.calledSensitiveFunction(),
            settings = settings,
        )
    }

    private fun toOllamaTools(
        functions: List<LLMFunction>?,
        parametersSchema: org.eclipse.lmos.arc.agents.functions.ParametersSchema? = null,
    ): List<Tool>? {
        return functions?.map { llmFunction ->
            Tool(
                type = "function",
                function = Function(
                    name = llmFunction.name,
                    description = llmFunction.description,
                    parameters = parametersSchema?.let { schema ->
                        Parameters(
                            type = "object",
                            properties = schema.properties.mapValues { (_, v) ->
                                Property(
                                    type = v.type,
                                    description = v.description ?: "",
                                    enum = v.enum,
                                )
                            },
                            required = schema.required ?: emptyList(),
                        )
                    },
                ),
            )
        }
    }

    private fun publishEvent(
        result: Result<AssistantMessage, ArcException>,
        messages: List<ConversationMessage>,
        functions: List<LLMFunction>?,
        chatCompletions: ChatResponse?,
        duration: Duration,
        functionCallHandler: FunctionCallHandler,
        settings: ChatCompletionSettings?,
    ) {
        eventHandler?.publish(
            LLMFinishedEvent(
                result,
                messages,
                functions,
                languageModel.modelName,
                chatCompletions?.let { it.promptEvalCount + it.evalCount } ?: -1,
                chatCompletions?.promptEvalCount ?: -1,
                chatCompletions?.evalCount ?: -1,
                functionCallHandler.calledFunctions.size,
                duration,
                settings = settings,
            ),
        )
    }

    private suspend fun chat(
        messages: List<ChatMessage>,
        tools: List<Tool>? = null,
        functionCallHandler: FunctionCallHandler,
        settings: ChatCompletionSettings?,
    ): Result<ChatResponse, ArcException> {
        val response: HttpResponse = client.post("${languageModel.url}/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(
                ChatRequest(
                    languageModel.modelName,
                    messages,
                    stream = false,
                    format = if (settings?.format == JSON) "json" else null,
                    tools = tools,
                ),
            )
        }.body()

        val chatResponse = response.takeIf { it.status.isSuccess() }
            ?.let { json.decodeFromString<ChatResponse>(it.bodyAsText()) }
            ?: throw ArcException(response.bodyAsText())

        val newMessages: List<ChatMessage> = functionCallHandler.handle(chatResponse).getOrThrow()
        if (newMessages.isNotEmpty()) {
            return chat(messages + newMessages, tools, functionCallHandler, settings)
        }
        return Success(chatResponse)
    }

    private suspend fun toOllamaMessages(messages: List<ConversationMessage>) =
        messages.map { msg ->
            when (msg) {
                is SystemMessage -> ChatMessage(
                    content = msg.content,
                    role = "system",
                )

                is AssistantMessage -> ChatMessage(
                    content = msg.content,
                    role = "assistant",
                )

                is UserMessage -> if (msg.binaryData.isNotEmpty()) {
                    ChatMessage(
                        content = msg.content,
                        role = "user",
                        images = msg.binaryData.map { it.readAllBytes().encodeBase64() },
                    )
                } else {
                    ChatMessage(content = msg.content, role = "user")
                }

                else -> throw ArcException("Unsupported message type: $msg")
            }
        }

    override suspend fun embed(texts: List<String>) = result<TextEmbeddings, ArcException> {
        val embeddings = texts.map { embed(it) failWith { it } }
        TextEmbeddings(embeddings)
    }

    private suspend fun embed(text: String) = result<TextEmbedding, ArcException> {
        val response: HttpResponse = client.post("${languageModel.url}/api/embeddings") {
            contentType(ContentType.Application.Json)
            setBody(TextEmbeddingRequest(languageModel.modelName, text))
        }.body()
        ensure(response.status.isSuccess()) { ArcException("Failed to create text embeddings: ${response.status}!") }
        TextEmbedding(
            text = text,
            embedding = response.bodyAsText().embedding.map { it.jsonPrimitive.double },
        )
    }

    private val String.embedding get() = json.parseToJsonElement(this).jsonObject["embedding"]!!.jsonArray

    private fun ChatResponse.getFirstAssistantMessage(
        sensitive: Boolean = false,
        settings: ChatCompletionSettings?,
    ) = AssistantMessage(
        message.content,
        sensitive = sensitive,
        format = when (settings?.format) {
            JSON -> MessageFormat.JSON
            else -> MessageFormat.TEXT
        },
    )
}
