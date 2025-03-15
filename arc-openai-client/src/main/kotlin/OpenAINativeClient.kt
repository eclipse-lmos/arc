// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.openai

import com.openai.client.OpenAIClientAsync
import com.openai.core.JsonValue
import com.openai.models.FunctionDefinition
import com.openai.models.FunctionParameters
import com.openai.models.ResponseFormatJsonObject
import com.openai.models.chat.completions.*
import com.openai.models.embeddings.EmbeddingCreateParams
import com.openai.models.embeddings.EmbeddingModel
import com.openai.models.responses.*
import kotlinx.coroutines.future.await
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
 * Calls the OpenAI endpoints and automatically handles LLM function calls.
 */
open class OpenAINativeClient(
    private val config: OpenAINativeClientConfig,
    private val client: OpenAIClientAsync,
    private val eventHandler: EventPublisher? = null,
) : ChatCompleter,
    TextEmbedder {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun complete(
        messages: List<ConversationMessage>,
        functions: List<LLMFunction>?,
        settings: ChatCompletionSettings?,
    ) =
        result<AssistantMessage, ArcException> {
            val functionCallHandler = FunctionCallHandler(functions ?: emptyList(), eventHandler)
            eventHandler?.publish(LLMStartedEvent(config.modelName))

            if (settings?.api == "completions") {
                val openAIMessages = toOpenAIMessages(messages)
                val openAIFunctions = if (functions != null) toOpenAIFunctions(functions) else null
                val result: Result<ChatCompletion, ArcException>
                val duration = measureTime {
                    result = getChatCompletions(openAIMessages, openAIFunctions, functionCallHandler, settings)
                }

                var chatCompletions: ChatCompletion? = null
                finally {
                    publishEvent(
                        it,
                        messages,
                        functions,
                        chatCompletions,
                        duration,
                        functionCallHandler,
                        settings
                    )
                }
                chatCompletions = result failWith { it }
                chatCompletions.getFirstAssistantMessage(
                    sensitive = functionCallHandler.calledSensitiveFunction(),
                    settings = settings,
                )
            } else {
                val responseAPIMessages = toOpenAIResponsesMessages(messages)
                val responseAPIFunction = toResponseAPIFunctions(functions ?: emptyList())
                val result: Result<Response, ArcException>
                val duration = measureTime {
                    result = getResponses(responseAPIMessages, responseAPIFunction, functionCallHandler, settings)
                }

                var responses: Response? = null
                finally {
                    publishResponsesEvent(
                        it,
                        messages,
                        functions,
                        responses,
                        duration,
                        functionCallHandler,
                        settings
                    )
                }
                responses = result failWith { it }
                responses.output()[0].message().get().content().let {
                    AssistantMessage(
                        it[0].outputText().get().text(),
                        sensitive = functionCallHandler.calledSensitiveFunction(),
                        format = when (settings?.format) {
                            JSON -> MessageFormat.JSON
                            else -> MessageFormat.TEXT
                        },
                    )
                }
            }
        }

    private fun publishEvent(
        result: Result<AssistantMessage, ArcException>,
        messages: List<ConversationMessage>,
        functions: List<LLMFunction>?,
        chatCompletions: ChatCompletion?,
        duration: Duration,
        functionCallHandler: FunctionCallHandler,
        settings: ChatCompletionSettings?,
    ) {
        eventHandler?.publish(
            LLMFinishedEvent(
                result,
                messages,
                functions,
                config.modelName,
                chatCompletions?.usage()?.get()?.totalTokens()?.toInt() ?: -1,
                chatCompletions?.usage()?.get()?.promptTokens()?.toInt() ?: -1,
                chatCompletions?.usage()?.get()?.completionTokens()?.toInt() ?: -1,
                functionCallHandler.calledFunctions.size,
                duration,
                settings = settings,
            ),
        )
    }

    private fun publishResponsesEvent(
        result: Result<AssistantMessage, ArcException>,
        messages: List<ConversationMessage>,
        functions: List<LLMFunction>?,
        responses: Response?,
        duration: Duration,
        functionCallHandler: FunctionCallHandler,
        settings: ChatCompletionSettings?,
    ) {
        eventHandler?.publish(
            LLMFinishedEvent(
                result,
                messages,
                functions,
                config.modelName,
                totalTokens = responses?.usage()?.get()?.totalTokens()?.toInt() ?: -1,
                promptTokens = responses?.usage()?.get()?.inputTokens()?.toInt() ?: -1,
                completionTokens = responses?.usage()?.get()?.outputTokens()?.toInt() ?: -1,
                functionCallHandler.calledFunctions.size,
                duration,
                settings = settings,
            ),
        )
    }

    private fun ChatCompletion.getFirstAssistantMessage(
        sensitive: Boolean = false,
        settings: ChatCompletionSettings?,
    ) = choices().first().message().content().get().let {
        AssistantMessage(
            it,
            sensitive = sensitive,
            format = when (settings?.format) {
                JSON -> MessageFormat.JSON
                else -> MessageFormat.TEXT
            },
        )
    }

    private suspend fun getChatCompletions(
        messages: List<ChatCompletionMessageParam>,
        openAIFunctions: List<ChatCompletionTool>? = null,
        functionCallHandler: FunctionCallHandler,
        settings: ChatCompletionSettings?,
    ): Result<ChatCompletion, ArcException> {
        val params = ChatCompletionCreateParams.builder()
            .model(config.modelName)
            .messages(messages)
            .tools(openAIFunctions ?: listOf())
            .apply {
                settings?.temperature?.let { temperature(it) }
                settings?.topP?.let { topP(it) }
                settings?.seed?.let { seed(it) }
                settings?.n?.let { n(it.toLong()) }
                settings?.maxTokens?.let { maxTokens(it.toLong()) }
                settings?.format?.takeIf { JSON == it }?.let {
                    responseFormat(
                        ResponseFormatJsonObject.builder().type(JsonValue.from("json_object")).build())
                }
            }.build()

        val chatCompletions = try {
            client.chat().completions().create(params).await()
        } catch (ex: Exception) {
            log.error("Call to OpenAI failed!", ex)
            return Failure(mapOpenAIException(ex))
        }

        log.debug("ChatCompletions: ${chatCompletions.choices()[0].finishReason()} (${chatCompletions.choices().size})")

        val newMessages = functionCallHandler.handle(chatCompletions).getOrThrow()
        if (newMessages.isNotEmpty()) {
            return getChatCompletions(messages + newMessages, openAIFunctions, functionCallHandler, settings)
        }
        return Success(chatCompletions)
    }

    private suspend fun getResponses(
        messages: List<ResponseInputItem>,
        openAIFunctions: List<Tool>? = null,
        functionCallHandler: FunctionCallHandler,
        settings: ChatCompletionSettings?,
    ): Result<Response, ArcException> {
        val params = ResponseCreateParams.builder()
            .model(config.modelName)
            .input(ResponseCreateParams.Input.ofResponse(messages))
            .tools(openAIFunctions ?: listOf())
            .store(false)
            .apply {
                settings?.format?.takeIf { JSON == it }?.let {
                    this.text(
                        ResponseTextConfig.builder()
                            .format(
                                ResponseFormatTextConfig.ofJsonObject(
                                    ResponseFormatJsonObject.builder()
                                        .type(JsonValue.from("json_object"))
                                        .build()
                                )
                            )
                            .build()
                    )
                }
            }
            .build()

        val responses = try {
            client.responses().create(params).await()
        } catch (ex: Exception) {
            log.error("Call to OpenAI responses failed!", ex)
            return Failure(mapOpenAIException(ex))
        }

        log.debug("Responses: ${responses.output()[0].message()} (${responses.output().size})")

        val newMessages = functionCallHandler.handleResponse(responses).getOrThrow()
        if (newMessages.isNotEmpty()) {
            return getResponses(messages + newMessages, openAIFunctions, functionCallHandler, settings)
        }
        return Success(responses)
    }

    private fun mapOpenAIException(ex: Exception): ArcException {
        return ArcException(ex.message ?: "Unexpected error!", ex)
    }

    private fun toOpenAIMessages(messages: List<ConversationMessage>) = messages.map { msg ->
        when (msg) {
            is UserMessage -> ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder()
                    .role(JsonValue.from("user"))
                    .content(msg.content).build(),
            )

            is SystemMessage -> ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder()
                    .role(JsonValue.from("system"))
                    .content(msg.content).build(),
            )

            is AssistantMessage -> ChatCompletionMessageParam.ofAssistant(
                ChatCompletionAssistantMessageParam.builder()
                    .role(JsonValue.from("assistant"))
                    .content(msg.content).build(),
            )

            is DeveloperMessage -> ChatCompletionMessageParam.ofDeveloper(
                ChatCompletionDeveloperMessageParam.builder()
                    .role(JsonValue.from("developer"))
                    .content(msg.content).build(),
            )
        }
    }

    private fun toOpenAIResponsesMessages(messages: List<ConversationMessage>) = messages.map { msg ->
        val role = when (msg) {
            is UserMessage -> "user"
            is SystemMessage -> "system"
            is AssistantMessage -> "assistant"
            is DeveloperMessage -> "developer"
        }

        ResponseInputItem.ofMessage(
            ResponseInputItem.Message.builder()
                .role(ResponseInputItem.Message.Role.of(role))
                .addInputTextContent(msg.content)
                .build()
        )
    }

    /**
     * Converts functions to openai functions.
     */
    private fun toOpenAIFunctions(functions: List<LLMFunction>) = functions.map { fn ->
        val jsonObject = fn.parameters.toOpenAISchemaAsMap()
        ChatCompletionTool.builder()
            .type(JsonValue.from("function"))
            .function(
                FunctionDefinition.builder()
                    .name(fn.name).description(fn.description).parameters(
                        FunctionParameters.builder().putAdditionalProperty("type", JsonValue.from(jsonObject["type"]))
                            .putAdditionalProperty("properties", JsonValue.from(jsonObject["properties"]))
                            .putAdditionalProperty("required", JsonValue.from(jsonObject["required"])).build(),
                    ).build(),
            ).build()
    }.takeIf { it.isNotEmpty() }

    private fun toResponseAPIFunctions(functions: List<LLMFunction>) = functions.map { fn ->
        val jsonObject = fn.parameters.toOpenAISchemaAsMap()
        when (fn.name) {
            "file_search" -> throw ArcException("File search is not supported!") //Need to think through arc function definition point of view. Skipping for now.
            "computer_use_preview" -> throw ArcException("Computer use preview is not supported!") //Need to think through arc function definition point of view. Skipping for now.
            "web_search_preview" -> {
                Tool.ofWebSearch(WebSearchTool.builder().build())
            }
            else -> {
                Tool.ofFunction(
                    FunctionTool.builder()
                        .name(fn.name)
                        .parameters(
                            FunctionTool.Parameters.builder()
                                .putAdditionalProperty("type", JsonValue.from(jsonObject["type"]))
                                .putAdditionalProperty("properties", JsonValue.from(jsonObject["properties"]))
                                .putAdditionalProperty("required", JsonValue.from(jsonObject["required"]))
                                .build()
                        )
                        .build()
                )
            }
        }
    }.takeIf { it.isNotEmpty() }

    override suspend fun embed(texts: List<String>) = result<TextEmbeddings, Exception> {
        EmbeddingCreateParams.Body.builder().model(EmbeddingModel.of(config.modelName)).build().toBuilder()
        val embedding = client.embeddings()
            .create(EmbeddingCreateParams.builder().model(EmbeddingModel.of(config.modelName)).build()).await().let { result ->
                result.data().map { e -> TextEmbedding(texts[e.index().toInt()], e.embedding()) }
            }
        TextEmbeddings(embedding)
    }.mapFailure { ArcException("Failed to create text embeddings!", it) }
}
