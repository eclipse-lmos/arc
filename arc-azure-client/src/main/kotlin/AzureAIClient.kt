// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.azure

import com.azure.ai.openai.OpenAIAsyncClient
import com.azure.ai.openai.models.*
import com.azure.core.exception.ClientAuthenticationException
import com.azure.core.util.BinaryData
import kotlinx.coroutines.reactive.awaitFirst
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
class AzureAIClient(
    private val config: AzureClientConfig,
    private val client: OpenAIAsyncClient,
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
            val result = getChatCompletions(
                openAIMessages,
                openAIFunctions,
                functionCallHandler,
                settings
            ) { result, chatCompletions, duration, settings ->
                publishEvent(result, messages, functions, chatCompletions, duration, settings)
            }
            result failWith { it }
        }

    private fun publishEvent(
        result: Result<AssistantMessage, ArcException>,
        messages: List<ConversationMessage>,
        functions: List<LLMFunction>?,
        chatCompletions: ChatCompletions?,
        duration: Duration,
        settings: ChatCompletionSettings?,
    ) {
        eventHandler?.publish(
            LLMFinishedEvent(
                result,
                messages,
                functions,
                config.modelName,
                chatCompletions?.usage?.totalTokens ?: -1,
                chatCompletions?.usage?.promptTokens ?: -1,
                chatCompletions?.usage?.completionTokens ?: -1,
                chatCompletions?.choices?.getOrNull(0)?.message?.toolCalls?.size ?: 0,
                duration,
                settings = settings,
            ),
        )
    }

    private fun ChatCompletions.getFirstAssistantMessage(
        sensitive: Boolean = false,
        settings: ChatCompletionSettings?,
    ) = choices.first().message.content.let {
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
        messages: List<ChatRequestMessage>,
        openAIFunctions: List<ChatCompletionsFunctionToolDefinition>? = null,
        functionCallHandler: FunctionCallHandler,
        settings: ChatCompletionSettings?,
        publishCompletionEvent: (
            result: Result<AssistantMessage, ArcException>,
            chatCompletions: ChatCompletions?,
            duration: Duration,
            settings: ChatCompletionSettings?,
        ) -> Unit,
    ): Result<AssistantMessage, ArcException> {
        val chatCompletionsOptions = toCompletionsOptions(messages, openAIFunctions, settings)

        val (chatCompletionsResult, duration) = doChatCompletions(chatCompletionsOptions)
        val result = chatCompletionsResult.map {
            it.getFirstAssistantMessage(
                sensitive = functionCallHandler.calledSensitiveFunction(),
                settings = settings,
            )
        }

        publishCompletionEvent(result, chatCompletionsResult.getOrNull(), duration, settings)

        chatCompletionsResult.getOrNull()?.let { chatCompletions ->
            log.debug("ChatCompletions: ${chatCompletions.choices[0].finishReason} (${chatCompletions.choices.size})")
            val newMessages = functionCallHandler.handle(chatCompletions).getOrThrow()
            if (newMessages.isNotEmpty()) {
                return getChatCompletions(
                    messages + newMessages,
                    openAIFunctions,
                    functionCallHandler,
                    settings,
                    publishCompletionEvent,
                )
            }
        }
        return result
    }

    private suspend fun doChatCompletions(chatCompletionsOptions: ChatCompletionsOptions): Pair<Result<ChatCompletions, ArcException>, Duration> {
        val result: Result<ChatCompletions, ArcException>
        val duration = measureTime {
            result = result<ChatCompletions, ArcException> {
                client.getChatCompletions(config.modelName, chatCompletionsOptions).awaitFirst()
            }.mapFailure {
                log.error("Calling Azure OpenAI failed!", it)
                mapOpenAIException(it)
            }
        }
        return result to duration
    }

    private fun toCompletionsOptions(
        messages: List<ChatRequestMessage>,
        openAIFunctions: List<ChatCompletionsFunctionToolDefinition>? = null,
        settings: ChatCompletionSettings?,
    ) = ChatCompletionsOptions(messages)
        .apply {
            settings?.temperature?.let { temperature = it }
            settings?.topP?.let { topP = it }
            settings?.seed?.let { seed = it }
            settings?.n?.let { n = it }
            settings?.maxTokens?.let { maxTokens = it }
            settings?.format?.takeIf { JSON == it }?.let { responseFormat = ChatCompletionsJsonResponseFormat() }
            if (openAIFunctions != null) tools = openAIFunctions
        }

    private fun mapOpenAIException(ex: Exception): ArcException = when (ex) {
        is ClientAuthenticationException -> ArcException(ex.message ?: "Unexpected error!", ex)
        else -> ArcException(ex.message ?: "Unexpected error!", ex)
    }

    private fun toOpenAIMessages(messages: List<ConversationMessage>) = messages.map { msg ->
        when (msg) {
            is UserMessage -> ChatRequestUserMessage(msg.content)
            is SystemMessage -> ChatRequestSystemMessage(msg.content)
            is AssistantMessage -> ChatRequestAssistantMessage(msg.content)
            else -> throw ArcException("Unsupported message type: $msg")
        }
    }

    /**
     * Converts functions to openai functions.
     */
    private fun toOpenAIFunctions(functions: List<LLMFunction>) = functions.map { fn ->
        ChatCompletionsFunctionToolDefinition(
            ChatCompletionsFunctionToolDefinitionFunction(fn.name).apply {
                description = fn.description
                parameters = BinaryData.fromObject(fn.parameters.toAzureOpenAISchemaAsMap())
            },
        )
    }.takeIf { it.isNotEmpty() }

    override suspend fun embed(texts: List<String>) = result<TextEmbeddings, Exception> {
        val embedding = client.getEmbeddings(config.modelName, EmbeddingsOptions(texts)).awaitFirst().let { result ->
            result.data.map { e -> TextEmbedding(texts[e.promptIndex], e.embedding.map { it.toDouble() }) }
        }
        TextEmbeddings(embedding)
    }.mapFailure { ArcException("Failed to create text embeddings!", it) }
}
