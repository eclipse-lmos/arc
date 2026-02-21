// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.openai


import com.openai.core.JsonValue
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam
import com.openai.models.chat.completions.ChatCompletionDeveloperMessageParam
import com.openai.models.chat.completions.ChatCompletionMessageParam
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam
import com.openai.models.chat.completions.ChatCompletionUserMessageParam
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.MissingModelNameException
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.conversation.DeveloperMessage
import org.eclipse.lmos.arc.agents.conversation.MessageFormat
import org.eclipse.lmos.arc.agents.conversation.SystemMessage
import org.eclipse.lmos.arc.agents.conversation.ToolCall
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.toJsonMap
import org.eclipse.lmos.arc.agents.llm.AIClientConfig
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.llm.LLMStartedEvent
import org.eclipse.lmos.arc.agents.llm.OutputFormat.JSON
import org.eclipse.lmos.arc.agents.llm.OutputSchema
import org.eclipse.lmos.arc.agents.llm.ReasoningEffort
import org.eclipse.lmos.arc.agents.llm.ReasoningEffort.HIGH
import org.eclipse.lmos.arc.agents.llm.ReasoningEffort.LOW
import org.eclipse.lmos.arc.agents.llm.ReasoningEffort.MEDIUM
import org.eclipse.lmos.arc.agents.llm.TextEmbedder
import org.eclipse.lmos.arc.agents.llm.TextEmbedding
import org.eclipse.lmos.arc.agents.llm.TextEmbeddings
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.eclipse.lmos.arc.agents.tracing.DefaultAgentTracer
import org.eclipse.lmos.arc.agents.withLogContext
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.failWith
import org.eclipse.lmos.arc.core.getOrNull
import org.eclipse.lmos.arc.core.getOrThrow
import org.eclipse.lmos.arc.core.map
import org.eclipse.lmos.arc.core.mapFailure
import org.eclipse.lmos.arc.core.result
import org.slf4j.LoggerFactory
import com.openai.client.OpenAIClientAsync
import com.openai.models.FunctionDefinition
import com.openai.models.FunctionParameters
import com.openai.models.ResponseFormatJsonObject
import com.openai.models.chat.completions.*
import com.openai.models.embeddings.EmbeddingCreateParams
import com.openai.models.embeddings.EmbeddingModel
import kotlin.collections.map
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * Calls the OpenAI endpoints and automatically handles LLM function calls.
 */
class AzureAIClient(
    private val config: AIClientConfig,
    private val client: OpenAIClientAsync,
    private val globalEventPublisher: EventPublisher? = null,
    private val tracer: AgentTracer? = null,
) : ChatCompleter {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun complete(
        messages: List<ConversationMessage>,
        functions: List<LLMFunction>?,
        settings: ChatCompletionSettings?,
        eventPublisher: EventPublisher?,
    ) =
        result<AssistantMessage, ArcException> {
            val openAIMessages = toOpenAIMessages(messages)
            val openAIFunctions = if (functions != null) toOpenAIFunctions(functions) else null
            val eventHandler = eventPublisher ?: globalEventPublisher
            val functionCallHandler =
                FunctionCallHandler(functions ?: emptyList(), eventHandler, tracer = tracer ?: DefaultAgentTracer())
            val llmEventPublisher = LLMEventPublisher(config, functions, eventHandler, messages, settings)
            val model =
                config.modelName ?: settings?.deploymentNameOrModel() ?: failWith { MissingModelNameException() }

            eventHandler?.publish(LLMStartedEvent(model))
            val result = getChatCompletions(
                openAIMessages,
                openAIFunctions,
                functionCallHandler,
                settings,
                llmEventPublisher,
            )
            result failWith { it }
        }

    private fun ChatCompletion.getFirstAssistantMessage(
        sensitive: Boolean = false,
        settings: ChatCompletionSettings?,
        toolCalls: Map<String, ToolCall>,
    ) = choices().first().message().content().getOrNull().let {
        AssistantMessage(
            it ?: "",
            sensitive = sensitive,
            format = settings?.outputSchema?.let { MessageFormat.JSON } ?: when (settings?.format) {
                JSON -> MessageFormat.JSON
                else -> MessageFormat.TEXT
            },
            toolCalls = toolCalls.map { ToolCall(it.value.name, it.value.arguments) },
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
        val result = withLLMSpan(settings, messages, functionCallHandler) { tag ->
            val pair = doChatCompletions(
                chatCompletionsOptions,
                deploymentOrModelName = config.modelName ?: settings?.deploymentNameOrModel()!!,
            )
            chatCompletionsResult = pair.first
            duration = pair.second
            // chatCompletionsResult.onFailure { tag.error(it) } // TODO: add error tag
            chatCompletionsResult.getOrNull()?.let { tag(it) }
            chatCompletionsResult.map {
                it.getFirstAssistantMessage(
                    sensitive = functionCallHandler.calledSensitiveFunction(),
                    settings = settings,
                    toolCalls = functionCallHandler.calledFunctions,
                )
            }
        }
        llmEventPublisher.publishEvent(result, chatCompletionsResult.getOrNull(), duration)
        val useCase = result.getOrNull()?.content?.extractUseCaseId()

        return withLogContext(useCase?.let { mapOf("use_case" to useCase) } ?: emptyMap()) {
            chatCompletionsResult.getOrNull()?.let { chatCompletions ->
                log.debug("ChatCompletions: ${chatCompletions.choices[0].finishReason} (${chatCompletions.choices.size})")
                val newMessages = functionCallHandler.handle(chatCompletions).getOrThrow()
                if (newMessages.isNotEmpty()) {
                    // tool was called, continue the cycle
                    return@withLogContext getChatCompletions(
                        messages + newMessages,
                        openAIFunctions,
                        functionCallHandler,
                        settings,
                        llmEventPublisher,
                    )
                }
            }
            // llm has completed
            return@withLogContext result
        }
    }

    @OptIn(ExperimentalContracts::class)
    private suspend fun <T> withLLMSpan(
        settings: ChatCompletionSettings?,
        inputMessages: List<ChatRequestMessage>,
        functionCallHandler: FunctionCallHandler,
        fn: suspend ((ChatCompletion) -> Unit) -> T,
    ): T {
        contract {
            callsInPlace(fn, EXACTLY_ONCE)
        }
        return (tracer ?: DefaultAgentTracer()).withSpan("llm") { tags, _ ->
            fn({ completions ->
                // TODO
                val spec = System.getenv("OTEL_SPEC")
                if ("GEN_AI" == spec) {
                    GenAITags.applyAttributes(tags, config, settings, completions, inputMessages)
                } else {
                    OpenInferenceTags.applyAttributes(
                        tags,
                        config,
                        settings,
                        completions,
                        inputMessages,
                        functionCallHandler,
                    )
                }
            })
        }
    }

    private suspend fun doChatCompletions(
        chatCompletionsOptions: ChatCompletionCreateParams,
        deploymentOrModelName: String,
    ): Pair<Result<ChatCompletion, ArcException>, Duration> {
        val result: Result<ChatCompletion, ArcException>
        val duration = measureTime {
            result = result<ChatCompletion, ArcException> {
                client.chat().completions().create(chatCompletionsOptions).await()
            }.mapFailure {
                log.error("Calling Azure OpenAI failed!", it)
                mapOpenAIException(it)
            }
        }
        return result to duration
    }

    @OptIn(InternalSerializationApi::class)
    private fun OutputSchema.toSchema(): String {
        return (type.serializer().descriptor.toSchema() as Schema.ObjectSchema).copy(
            additionalProperties = JsonPrimitive(false),
        ).toJsonElement().toString()
    }

    private fun toCompletionsOptions(
        messages: List<ChatRequestMessage>,
        openAIFunctions: List<ChatCompletionsFunctionToolDefinition>? = null,
        settings: ChatCompletionSettings?,
    ) = ChatCompletionCreateParams.builder().messages(messages)
        .apply {
            settings?.outputSchema?.let { jsonSchema ->
                val schema = jsonSchema.toSchema()
                log.debug("Using output schema: $schema")
                responseFormat = ChatCompletionsJsonSchemaResponseFormat(
                    ChatCompletionsJsonSchemaResponseFormatJsonSchema(jsonSchema.name)
                        .setStrict(true)
                        .setDescription(jsonSchema.description)
                        .setSchema(fromString(schema)),
                )
            }
            settings?.temperature?.let { temperature = it }
            settings?.topP?.let { topP(it) }
            settings?.seed?.let { seed(it) }
            settings?.n?.let { n = it }
            settings?.model?.let { model = it }
            settings?.reasoningEffort?.let {
                reasoningEffort = when (it) {
                    LOW -> ReasoningEffortValue.LOW
                    MEDIUM -> ReasoningEffortValue.MEDIUM
                    HIGH -> ReasoningEffortValue.HIGH
                }
            }
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


    /**
     * Converts functions to openai functions.
     */
    private fun toOpenAIFunctions(functions: List<LLMFunction>) = functions.map { fn ->
        ChatCompletionsFunctionToolDefinition(
            ChatCompletionsFunctionToolDefinitionFunction(fn.name).apply {
                description = fn.description
                parameters = fromObject(fn.parameters.toJsonMap())
            },
        )
    }.takeIf { it.isNotEmpty() }

    override suspend fun embed(texts: List<String>) = result<TextEmbeddings, Exception> {
        val embedding = client.getEmbeddings(config.modelName, EmbeddingsOptions(texts)).awaitFirst().let { result ->
            result.data.map { e -> TextEmbedding(texts[e.promptIndex], e.embedding.map { it.toDouble() }) }
        }
        TextEmbeddings(embedding)
    }.mapFailure { ArcException("Failed to create text embeddings!", it) }

    override fun toString(): String {
        return "OpenAIClient(config=$config, client=$client)"
    }
}

/**
 * Todo come up with a better way to pass the use case id to the client
 */
private val useCaseIdRegex = "<ID:(.*?)>".toRegex(RegexOption.IGNORE_CASE)

fun String.extractUseCaseId(): String? {
    return try {
        useCaseIdRegex.find(this)?.groupValues?.elementAtOrNull(1)?.trim()
    } catch (ex: Exception) {
        null
    }
}
