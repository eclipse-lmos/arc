// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.openai


import com.openai.core.JsonValue
import com.openai.models.chat.completions.ChatCompletion
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.openai.models.chat.completions.ChatCompletionMessageParam
import com.openai.models.chat.completions.ChatCompletionTool
import com.openai.models.chat.completions.ChatCompletionUserMessageParam
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam
import com.openai.models.chat.completions.ChatCompletionDeveloperMessageParam
import com.openai.models.FunctionDefinition
import com.openai.models.ResponseFormatJsonObject
import com.openai.models.embeddings.EmbeddingCreateParams
import com.openai.client.OpenAIClientAsync
import kotlinx.coroutines.future.await
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
import kotlin.collections.map
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration
import kotlin.time.measureTime

// import sh.ondr.koja.Schema
// import sh.ondr.koja.toSchema

/**
 * Calls the OpenAI endpoints and automatically handles LLM function calls.
 */
class OpenAIClient(
    private val config: AIClientConfig,
    private val client: OpenAIClientAsync,
    private val globalEventPublisher: EventPublisher? = null,
    private val tracer: AgentTracer? = null,
) : ChatCompleter, TextEmbedder {

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
        messages: List<ChatCompletionMessageParam>,
        openAIFunctions: List<ChatCompletionTool>? = null,
        functionCallHandler: FunctionCallHandler,
        settings: ChatCompletionSettings?,
        llmEventPublisher: LLMEventPublisher,
    ): Result<AssistantMessage, ArcException> {
        val chatCompletionsOptions = toCompletionsOptions(messages, openAIFunctions, settings)

        var chatCompletionsResult: Result<ChatCompletion, ArcException>
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
                    toolCalls = functionCallHandler.calledFunctions.mapValues { ToolCall(it.value.name, it.value.arguments) },
                )
            }
        }
        llmEventPublisher.publishEvent(result, chatCompletionsResult.getOrNull(), duration)
        val useCase = result.getOrNull()?.content?.extractUseCaseId()

        return withLogContext(useCase?.let { mapOf("use_case" to useCase) } ?: emptyMap()) {
            chatCompletionsResult.getOrNull()?.let { chatCompletions ->
                log.debug("ChatCompletions: ${chatCompletions.choices().get(0).finishReason()} (${chatCompletions.choices().size})")
                val newMessages = functionCallHandler.handle(chatCompletions).getOrThrow()
                if (newMessages.isNotEmpty()) {
                    // tool was called, continue the cycle
                    // functionCallHandler.handle returns List<ChatCompletionMessageParam>
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
        inputMessages: List<ChatCompletionMessageParam>,
        functionCallHandler: FunctionCallHandler,
        fn: suspend ((ChatCompletion) -> Unit) -> T,
    ): T {
        contract {
            callsInPlace(fn, EXACTLY_ONCE)
        }
        return (tracer ?: DefaultAgentTracer()).withSpan("llm") { tags, _ ->
            fn({ completions ->
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

//    @OptIn(InternalSerializationApi::class)
//    private fun OutputSchema.toSchema(): String {
//        return (type.serializer().descriptor.toSchema() as Schema.ObjectSchema).copy(
//            additionalProperties = JsonPrimitive(false),
//        ).toJsonElement().toString()
//    }

    private fun toCompletionsOptions(
        messages: List<ChatCompletionMessageParam>,
        openAIFunctions: List<ChatCompletionTool>? = null,
        settings: ChatCompletionSettings?,
    ): ChatCompletionCreateParams {
        val builder = ChatCompletionCreateParams.builder()
            .messages(messages)
            .model(config.modelName ?: settings?.deploymentNameOrModel() ?: throw MissingModelNameException())

//        settings?.outputSchema?.let { jsonSchema ->
//            val schema = jsonSchema.toSchema()
//            log.debug("Using output schema: $schema")
//            builder.responseFormat(
//                ResponseFormatJsonSchema.builder()
//                    .type(ResponseFormatJsonSchema.Type.JSON_SCHEMA)
//                    .jsonSchema(
//                        ResponseFormatJsonSchema.JsonSchema.builder()
//                            .name(jsonSchema.name)
//                            .strict(true)
//                            .description(jsonSchema.description)
//                            .schema(JsonValue.from(mapOf("schema" to schema))) // adjustable
//                            .build()
//                    )
//                    .build()
//            )
//        }
        settings?.temperature?.let { builder.temperature(it) }
        settings?.topP?.let { builder.topP(it) }
        settings?.seed?.let { builder.seed(it) }
        settings?.n?.let { builder.n(it.toLong()) }
        settings?.model?.let { builder.model(it) }
        settings?.reasoningEffort?.let {
            builder.reasoningEffort(
                when (it) {
                    LOW -> com.openai.models.ReasoningEffort.LOW
                    MEDIUM -> com.openai.models.ReasoningEffort.MEDIUM
                    HIGH -> com.openai.models.ReasoningEffort.HIGH
                }
            )
        }
        settings?.maxTokens?.let { builder.maxTokens(it.toLong()) }

        if (settings?.format == JSON) {
             builder.responseFormat(
                 ResponseFormatJsonObject.builder()
                     .type(JsonValue.from("json_object"))
                     .build()
             )
        }

        if (openAIFunctions != null) {
            builder.tools(openAIFunctions)
        }

        return builder.build()
    }

    private fun mapOpenAIException(ex: Exception): ArcException = when (ex) {
        // is ClientAuthenticationException -> ArcException(ex.message ?: "Unexpected error!", ex) // Check correct exception class
        else -> ArcException(ex.message ?: "Unexpected error!", ex)
    }

    private fun toOpenAIMessages(messages: List<ConversationMessage>): List<ChatCompletionMessageParam> = messages.map { msg ->
        when (msg) {
            is UserMessage -> ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder()
                    .role(JsonValue.from("user"))
                    .content(ChatCompletionUserMessageParam.Content.ofText(msg.content)).build(),
            )

            is SystemMessage -> ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder()
                    .role(JsonValue.from("system"))
                    .content(ChatCompletionSystemMessageParam.Content.ofText(msg.content)).build(),
            )

            is AssistantMessage -> ChatCompletionMessageParam.ofAssistant(
                ChatCompletionAssistantMessageParam.builder()
                    .role(JsonValue.from("assistant"))
                    .content(ChatCompletionAssistantMessageParam.Content.ofText(msg.content)).build(),
            )

            is DeveloperMessage -> ChatCompletionMessageParam.ofDeveloper(
                ChatCompletionDeveloperMessageParam.builder()
                    .role(JsonValue.from("developer"))
                    .content(ChatCompletionDeveloperMessageParam.Content.ofText(msg.content)).build(),
            )
        }
    }


    /**
     * Converts functions to openai functions.
     */
    private fun toOpenAIFunctions(functions: List<LLMFunction>) = functions.map { fn ->
        val jsonObject = fn.parameters.toJsonMap() // Assuming toJsonMap returns Map<String, Any>
        ChatCompletionTool.builder()
            .type(JsonValue.from("function"))
            .function(
                FunctionDefinition.builder()
                    .name(fn.name)
                    .description(fn.description)
                    .parameters(JsonValue.from(jsonObject))
//                    .parameters(
//                        FunctionParameters.builder()
//                             // TODO: deeply convert map to FunctionParameters or use JsonValue if supported
//                             // Native client uses JsonValue for parameters? No, FunctionParameters builder
//                            .build()
//                    )
                    .build()
            )
            .build()
    }.takeIf { it.isNotEmpty() }

    override suspend fun embed(texts: List<String>) = result<TextEmbeddings, Exception> {
         val result = client.embeddings().create(
             EmbeddingCreateParams.builder()
                 .model(config.modelName ?: "text-embedding-3-small") // Fallback or throw
                 .input(EmbeddingCreateParams.Input.ofArrayOfStrings(texts))
                 .build()
         ).await()

        val embeddings = result.data().mapIndexed { index, embedding ->
            TextEmbedding(texts[index], embedding.embedding().map { it.toDouble() })
        }
        TextEmbeddings(embeddings)
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
