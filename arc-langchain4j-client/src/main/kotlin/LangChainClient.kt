// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.langchain4j

import dev.langchain4j.agent.tool.ToolSpecification
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.AudioContent
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.Content
import dev.langchain4j.data.message.ImageContent
import dev.langchain4j.data.message.TextContent
import dev.langchain4j.data.message.VideoContent
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.request.json.JsonArraySchema
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema
import dev.langchain4j.model.chat.request.json.JsonEnumSchema
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema
import dev.langchain4j.model.chat.request.json.JsonNumberSchema
import dev.langchain4j.model.chat.request.json.JsonObjectSchema
import dev.langchain4j.model.chat.request.json.JsonSchemaElement
import dev.langchain4j.model.chat.request.json.JsonStringSchema
import dev.langchain4j.model.output.Response
import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.MissingModelNameException
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.BinaryData
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.conversation.SystemMessage
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.ParameterSchema
import org.eclipse.lmos.arc.agents.llm.AIClientConfig
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.llm.LLMStartedEvent
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.eclipse.lmos.arc.agents.tracing.addLLMTags
import org.eclipse.lmos.arc.agents.tracing.spanLLMCall
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.failWith
import org.eclipse.lmos.arc.core.getOrThrow
import org.eclipse.lmos.arc.core.mapFailure
import org.eclipse.lmos.arc.core.result
import org.slf4j.LoggerFactory
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.measureTime

/**
 * Wraps a LangChain4j ChatLanguageModel to provide a ChatCompleter interface.
 */
class LangChainClient(
    private val config: AIClientConfig,
    private val clientBuilder: (AIClientConfig, ChatCompletionSettings?) -> ChatLanguageModel,
    private val eventHandler: EventPublisher? = null,
    private val tracer: AgentTracer? = null,
) : ChatCompleter {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun complete(
        messages: List<ConversationMessage>,
        functions: List<LLMFunction>?,
        settings: ChatCompletionSettings?,
    ) = result<AssistantMessage, ArcException> {
        val langChainMessages = toLangChainMessages(messages)
        val langChainFunctions = if (functions != null) toLangChainFunctions(functions) else null
        val functionCallHandler = FunctionCallHandler(functions ?: emptyList(), eventHandler, tracer)
        val llmEventPublisher = LLMEventPublisher(config, functions, eventHandler, messages, settings)
        val model =
            config.modelName ?: settings?.deploymentNameOrModel() ?: failWith { MissingModelNameException() }

        eventHandler?.publish(LLMStartedEvent(model))

        val result = chat(langChainMessages, langChainFunctions, settings, functionCallHandler, llmEventPublisher)
        result failWith { it }
    }

    private suspend fun chat(
        messages: List<ChatMessage>,
        langChainFunctions: List<ToolSpecification>? = null,
        settings: ChatCompletionSettings?,
        functionCallHandler: FunctionCallHandler,
        eventPublisher: LLMEventPublisher,
    ): Result<AssistantMessage, ArcException> {
        return try {
            val client = clientBuilder(config, settings)
            val result: Result<AssistantMessage, ArcException>
            var response: Response<AiMessage>? = null

            // Call the LLM
            val duration = measureTime {
                tracer.spanLLMCall { tags, _ ->
                    result = result<AssistantMessage, Exception> {
                        response = if (langChainFunctions?.isNotEmpty() == true) {
                            client.generate(messages, langChainFunctions)
                        } else {
                            client.generate(messages)
                        }
                        val output = AssistantMessage(
                            response!!.content().text(),
                            sensitive = functionCallHandler.calledSensitiveFunction(),
                        )
                        tags.addLLMTags(
                            config,
                            settings,
                            messages.toConversationMessages(),
                            listOf(output),
                            functionCallHandler.functions,
                            response!!.tokenUsage().toUsage(),
                        )
                        output
                    }.mapFailure {
                        tags.error(it)
                        ArcException("Failed to call LLM!", it)
                    }
                }
            }
            eventPublisher.publishEvent(result, response, duration, functionCallHandler)

            // Return if the result is a failures
            if (result is Failure) {
                return result
            }
            log.debug("ChatCompletions: ${response?.finishReason()} (${response?.content()?.toolExecutionRequests()})")

            // Check if the response contains any function calls
            val newMessages = functionCallHandler.handle(response!!.content()).getOrThrow()
            return if (newMessages.isNotEmpty()) {
                chat(messages + newMessages, langChainFunctions, settings, functionCallHandler, eventPublisher)
            } else {
                result
            }
        } catch (e: ArcException) {
            Failure(e)
        } catch (e: Exception) {
            Failure(ArcException("Failed to call LLM!", e))
        }
    }

    private suspend fun toLangChainMessages(messages: List<ConversationMessage>) =
        messages.map {
            when (it) {
                is UserMessage -> {
                    if (it.binaryData.isNotEmpty()) {
                        dev.langchain4j.data.message.UserMessage.from(
                            listOf(TextContent.from(it.content)) +
                                it.binaryData.map { data -> data.toContent() },
                        )
                    } else {
                        dev.langchain4j.data.message.UserMessage(it.content)
                    }
                }

                is AssistantMessage -> dev.langchain4j.data.message.AiMessage(it.content)
                is SystemMessage -> dev.langchain4j.data.message.SystemMessage(it.content)
                else -> error("Unsupported message type: $it")
            }
        }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun BinaryData.toContent(): Content {
        return when {
            mimeType.startsWith("audio/") -> AudioContent.from(Base64.encode(readAllBytes()), mimeType)
            mimeType.startsWith("video/") -> VideoContent.from(Base64.encode(readAllBytes()), mimeType)
            mimeType.startsWith("image/") -> ImageContent.from(Base64.encode(readAllBytes()), mimeType)
            else -> error("Unsupported binary data type: $mimeType!")
        }
    }

    /**
     * Converts functions to openai functions.
     */
    private fun toLangChainFunctions(functions: List<LLMFunction>) = functions.map { fn ->
        ToolSpecification.builder()
            .name(fn.name)
            .description(fn.description)
            .parameters(
                JsonObjectSchema.builder()
                    .apply {
                        properties(fn.parameters.properties.mapValues { (_, v) -> v.toJsonElement() })
                        required(fn.parameters.required)
                    }
                    .build(),
            )
            .build()
    }.takeIf { it.isNotEmpty() }

    private fun ParameterSchema.toJsonElement(): JsonSchemaElement {
        if (enum != null) return JsonEnumSchema.builder().description(description).enumValues(enum).build()
        return when (type) {
            "string" -> JsonStringSchema.builder().description(description).build()
            "integer" -> JsonIntegerSchema.builder().description(description).build()
            "number" -> JsonNumberSchema.builder().description(description).build()
            "boolean" -> JsonBooleanSchema.builder().description(description).build()

            "array" -> JsonArraySchema.builder().apply {
                description(description)
                items(items?.toJsonElement())
            }.build()

            "object" -> JsonObjectSchema.builder()
                .apply {
                    description(description)
                    properties(properties?.mapValues { it.value.toJsonElement() } ?: emptyMap())
                    required(required)
                }
                .build()

            else -> error("Unsupported parameter type: $type!")
        }
    }

    override fun toString(): String {
        return "LangChainClient(config=$config)"
    }
}
