// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.openai

import com.openai.client.OpenAIClientAsync
import com.openai.core.JsonValue
import com.openai.models.*
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
class OpenAINativeClient(
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
            val openAIMessages = toOpenAIMessages(messages)
            val openAIFunctions = if (functions != null) toOpenAIFunctions(functions) else null
            val functionCallHandler = FunctionCallHandler(functions ?: emptyList(), eventHandler)

            eventHandler?.publish(LLMStartedEvent(config.modelName))

            val result: Result<ChatCompletion, ArcException>
            val duration = measureTime {
                result = getChatCompletions(openAIMessages, openAIFunctions, functionCallHandler, settings)
            }

            var chatCompletions: ChatCompletion? = null
            finally { publishEvent(it, messages, functions, chatCompletions, duration, functionCallHandler, settings) }
            chatCompletions = result failWith { it }
            chatCompletions.getFirstAssistantMessage(
                sensitive = functionCallHandler.calledSensitiveFunction(),
                settings = settings,
            )
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
                        ChatCompletionCreateParams.ResponseFormat.ofResponseFormatJsonObject(
                            ResponseFormatJsonObject.builder().type(
                                ResponseFormatJsonObject.Type.JSON_OBJECT,
                            ).build(),
                        ),
                    )
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

    private fun mapOpenAIException(ex: Exception): ArcException {
        return ArcException(ex.message ?: "Unexpected error!", ex)
    }

    private fun toOpenAIMessages(messages: List<ConversationMessage>) = messages.map { msg ->
        when (msg) {
            is UserMessage -> ChatCompletionMessageParam.ofChatCompletionUserMessageParam(
                ChatCompletionUserMessageParam.builder()
                    .role(ChatCompletionUserMessageParam.Role.USER)
                    .content(ChatCompletionUserMessageParam.Content.ofTextContent(msg.content)).build(),
            )

            is SystemMessage -> ChatCompletionMessageParam.ofChatCompletionSystemMessageParam(
                ChatCompletionSystemMessageParam.builder()
                    .role(ChatCompletionSystemMessageParam.Role.SYSTEM)
                    .content(ChatCompletionSystemMessageParam.Content.ofTextContent(msg.content)).build(),
            )

            is AssistantMessage -> ChatCompletionMessageParam.ofChatCompletionAssistantMessageParam(
                ChatCompletionAssistantMessageParam.builder()
                    .role(ChatCompletionAssistantMessageParam.Role.ASSISTANT)
                    .content(ChatCompletionAssistantMessageParam.Content.ofTextContent(msg.content)).build(),
            )

            is DeveloperMessage -> ChatCompletionMessageParam.ofChatCompletionDeveloperMessageParam(
                ChatCompletionDeveloperMessageParam.builder()
                    .role(ChatCompletionDeveloperMessageParam.Role.DEVELOPER)
                    .content(ChatCompletionDeveloperMessageParam.Content.ofTextContent(msg.content)).build(),
            )
        }
    }

    /**
     * Converts functions to openai functions.
     */
    private fun toOpenAIFunctions(functions: List<LLMFunction>) = functions.map { fn ->
        val jsonObject = fn.parameters.toOpenAISchemaAsMap()
        ChatCompletionTool.builder()
            .type(ChatCompletionTool.Type.FUNCTION)
            .function(
                FunctionDefinition.builder()
                    .name(fn.name).description(fn.description).parameters(
                        FunctionParameters.builder().putAdditionalProperty("type", JsonValue.from(jsonObject["type"]))
                            .putAdditionalProperty("properties", JsonValue.from(jsonObject["properties"]))
                            .putAdditionalProperty("required", JsonValue.from(jsonObject["required"])).build(),
                    ).build(),
            ).build()
    }.takeIf { it.isNotEmpty() }

    override suspend fun embed(texts: List<String>) = result<TextEmbeddings, Exception> {
        EmbeddingCreateParams.EmbeddingCreateBody.builder().model(EmbeddingModel.of(config.modelName)).build().toBuilder()
        val embedding = client.embeddings()
            .create(EmbeddingCreateParams.builder().model(EmbeddingModel.of(config.modelName)).build()).await().let { result ->
                result.data().map { e -> TextEmbedding(texts[e.index().toInt()], e.embedding()) }
            }
        TextEmbeddings(embedding)
    }.mapFailure { ArcException("Failed to create text embeddings!", it) }
}
