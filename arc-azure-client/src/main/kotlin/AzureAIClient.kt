// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.azure

import com.azure.ai.openai.OpenAIAsyncClient
import com.azure.ai.openai.models.ChatCompletions
import com.azure.ai.openai.models.ChatCompletionsFunctionToolDefinition
import com.azure.ai.openai.models.ChatCompletionsFunctionToolDefinitionFunction
import com.azure.ai.openai.models.ChatCompletionsJsonResponseFormat
import com.azure.ai.openai.models.ChatCompletionsOptions
import com.azure.ai.openai.models.ChatRequestAssistantMessage
import com.azure.ai.openai.models.ChatRequestMessage
import com.azure.ai.openai.models.ChatRequestSystemMessage
import com.azure.ai.openai.models.ChatRequestUserMessage
import com.azure.ai.openai.models.EmbeddingsOptions
import com.azure.core.exception.ClientAuthenticationException
import com.azure.core.util.BinaryData
import kotlinx.coroutines.reactive.awaitFirst
import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.conversation.MessageFormat
import org.eclipse.lmos.arc.agents.conversation.SystemMessage
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.toJsonMap
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.llm.LLMStartedEvent
import org.eclipse.lmos.arc.agents.llm.OutputFormat.JSON
import org.eclipse.lmos.arc.agents.llm.TextEmbedder
import org.eclipse.lmos.arc.agents.llm.TextEmbedding
import org.eclipse.lmos.arc.agents.llm.TextEmbeddings
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.eclipse.lmos.arc.agents.tracing.DefaultAgentTracer
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.failWith
import org.eclipse.lmos.arc.core.getOrNull
import org.eclipse.lmos.arc.core.getOrThrow
import org.eclipse.lmos.arc.core.map
import org.eclipse.lmos.arc.core.mapFailure
import org.eclipse.lmos.arc.core.result
import org.slf4j.LoggerFactory
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * Calls the OpenAI endpoints and automatically handles LLM function calls.
 */
class AzureAIClient(
    private val config: AzureClientConfig,
    private val client: OpenAIAsyncClient,
    private val eventHandler: EventPublisher? = null,
    private val tracer: AgentTracer? = null,
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
            val llmEventPublisher = LLMEventPublisher(config, functions, eventHandler, messages, settings)

            eventHandler?.publish(LLMStartedEvent(config.modelName))
            val result = getChatCompletions(
                openAIMessages,
                openAIFunctions,
                functionCallHandler,
                settings,
                llmEventPublisher,
            )
            result failWith { it }
        }

    private fun ChatCompletions.getFirstAssistantMessage(
        sensitive: Boolean = false,
        settings: ChatCompletionSettings?,
    ) = choices.first().message.content.let {
        AssistantMessage(
            it ?: "",
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
        llmEventPublisher: LLMEventPublisher,
    ): Result<AssistantMessage, ArcException> {
        val chatCompletionsOptions = toCompletionsOptions(messages, openAIFunctions, settings)

        var chatCompletionsResult: Result<ChatCompletions, ArcException>
        var duration: Duration
        val result = withLLMSpan(settings, messages) { tag ->
            val pair = doChatCompletions(chatCompletionsOptions)
            chatCompletionsResult = pair.first
            duration = pair.second
            chatCompletionsResult.getOrNull()?.let { tag(it) }
            chatCompletionsResult.map {
                it.getFirstAssistantMessage(
                    sensitive = functionCallHandler.calledSensitiveFunction(),
                    settings = settings,
                )
            }
        }
        llmEventPublisher.publishEvent(result, chatCompletionsResult.getOrNull(), duration)

        chatCompletionsResult.getOrNull()?.let { chatCompletions ->
            log.debug("ChatCompletions: ${chatCompletions.choices[0].finishReason} (${chatCompletions.choices.size})")
            val newMessages = functionCallHandler.handle(chatCompletions).getOrThrow()
            if (newMessages.isNotEmpty()) {
                return getChatCompletions(
                    messages + newMessages,
                    openAIFunctions,
                    functionCallHandler,
                    settings,
                    llmEventPublisher,
                )
            }
        }
        return result
    }

    @OptIn(ExperimentalContracts::class)
    private suspend fun <T> withLLMSpan(
        settings: ChatCompletionSettings?,
        inputMessages: List<ChatRequestMessage>,
        fn: suspend ((ChatCompletions) -> Unit) -> T,
    ): T {
        contract {
            callsInPlace(fn, EXACTLY_ONCE)
        }
        val name = "chat ${config.modelName}"
        return (tracer ?: DefaultAgentTracer()).withSpan(name) { tags, _ ->
            fn({ completions ->
                tags.tag("gen_ai.request.model", config.modelName)
                tags.tag("gen_ai.operation.name", "chat")
                tags.tag(
                    "gen_ai.response.finish_reasons",
                    completions.choices.joinToString(
                        prefix = "[",
                        postfix = "]",
                        separator = ",",
                    ) { it.finishReason.toString() },
                )
                settings?.seed?.let { tags.tag("gen_ai.request.seed", it) }
                settings?.temperature?.let { tags.tag("gen_ai.request.temperature", it.toString()) }
                settings?.topP?.let { tags.tag("gen_ai.request.top_p", it.toString()) }
                // tags.tag("gen_ai.user.message", event.messages.last().content)
                // tags.tag("gen_ai.choice", event.result.getOrNull()?.content ?: "")
                tags.tag("gen_ai.usage.input_tokens", completions.usage.promptTokens.toLong())
                tags.tag("gen_ai.usage.output_tokens", completions.usage.completionTokens.toLong())
                tags.tag("gen_ai.openai.response.system_fingerprint", completions.systemFingerprint)
                OpenInference.applyAttributes(tags, config, settings, completions, inputMessages)
            })
        }
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
                parameters = BinaryData.fromObject(fn.parameters.toJsonMap())
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
