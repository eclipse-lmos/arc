// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.gemini

import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.conversation.SystemMessage
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.llm.LLMFinishedEvent
import org.eclipse.lmos.arc.agents.llm.LLMStartedEvent
import org.eclipse.lmos.arc.client.gemini.ByteMapper.map
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.failWith
import org.eclipse.lmos.arc.core.finally
import org.eclipse.lmos.arc.core.getOrThrow
import org.eclipse.lmos.arc.core.result
import com.google.cloud.vertexai.VertexAI
import com.google.cloud.vertexai.api.Content
import com.google.cloud.vertexai.api.FunctionDeclaration
import com.google.cloud.vertexai.api.GenerateContentResponse
import com.google.cloud.vertexai.api.GenerationConfig
import com.google.cloud.vertexai.api.Schema
import com.google.cloud.vertexai.api.Tool
import com.google.cloud.vertexai.api.Type
import com.google.cloud.vertexai.generativeai.ContentMaker.forRole
import com.google.cloud.vertexai.generativeai.GenerativeModel
import com.google.cloud.vertexai.generativeai.ResponseHandler
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * Calls a Gemini endpoint to complete a conversation.
 */
class GeminiClient(
    private val languageModel: GeminiClientConfig,
    private val vertexAi: VertexAI,
    private val eventHandler: EventPublisher? = null,
) : ChatCompleter {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun complete(
        messages: List<ConversationMessage>,
        functions: List<LLMFunction>?,
        settings: ChatCompletionSettings?,
    ) = result<AssistantMessage, ArcException> {
        val vertexMessages = toVertexMessage(messages.systemToUser()) // gemini does not support system message
        val vertexFunctions = functions?.let { toVertexFunctions(it) }
        val functionCallHandler = FunctionCallHandler(functions ?: emptyList(), eventHandler)

        eventHandler?.publish(LLMStartedEvent(languageModel.modelName))

        val result: Result<GenerateContentResponse, ArcException>
        val duration = measureTime {
            result = chat(vertexMessages, vertexFunctions, functionCallHandler, settings)
        }

        var response: GenerateContentResponse? = null
        finally { publishEvent(it, messages, functions, response, duration, functionCallHandler, settings) }
        response = result failWith { ArcException("Failed to call Gemini!", it) }
        AssistantMessage(ResponseHandler.getText(response), sensitive = functionCallHandler.calledSensitiveFunction())
    }

    private fun publishEvent(
        result: Result<AssistantMessage, ArcException>,
        messages: List<ConversationMessage>,
        functions: List<LLMFunction>?,
        response: GenerateContentResponse?,
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
                totalTokens = response?.usageMetadata?.totalTokenCount ?: -1,
                promptTokens = response?.usageMetadata?.promptTokenCount ?: -1,
                completionTokens = response?.usageMetadata?.candidatesTokenCount ?: -1,
                functionCallHandler.calledFunctions.size,
                duration,
                settings = settings,
            ),
        )
    }

    private suspend fun chat(
        messages: List<Content>,
        functions: List<Tool>?,
        functionCallHandler: FunctionCallHandler,
        settings: ChatCompletionSettings?,
    ): Result<GenerateContentResponse, ArcException> {
        val generationConfig = GenerationConfig.newBuilder()
            .apply { settings?.maxTokens?.let { maxOutputTokens = it } }
            .apply { settings?.temperature?.let { temperature = it.toFloat() } }
            .apply { settings?.topP?.let { topP = it.toFloat() } }
            .build()
        val model = GenerativeModel.Builder()
            .setModelName(languageModel.modelName)
            .setVertexAi(vertexAi)
            .setGenerationConfig(generationConfig)
            .apply { if (functions != null) setTools(functions) }
            .build()
        // TODO use async method
        val response = model.generateContent(messages)

        val newMessages = functionCallHandler.handle(response).getOrThrow()
        if (newMessages.isNotEmpty()) {
            return chat(messages + newMessages, functions, functionCallHandler, settings)
        }
        return Success(response)
    }

    fun toVertexFunctions(functions: List<LLMFunction>) = functions.map { function ->
        Tool.newBuilder().addFunctionDeclarations(
            FunctionDeclaration.newBuilder()
                .setName(function.name)
                .setDescription(function.description)
                .apply {
                    if (function.parameters.parameters.isNotEmpty()) {
                        setParameters(
                            Schema.newBuilder()
                                .setType(Type.OBJECT)
                                .apply {
                                    putAllProperties(
                                        function.parameters.parameters.associate { param ->
                                            // TODO add missing settings
                                            param.name to Schema.newBuilder()
                                                .setType(Type.STRING)
                                                .setDescription(param.description)
                                                .build()
                                        },
                                    )
                                },
                        )
                    }
                }
                .build(),
        ).build()
    }

    /**
     * Combines system messages with user messages.
     * This expects a single system message at the beginning of the list followed by a user message.
     */
    private fun List<ConversationMessage>.systemToUser(): List<ConversationMessage> {
        if (size < 2 || first() !is SystemMessage) return this
        val systemMessage = get(0)
        val userMessage = get(1)
        val combinedUserMessage = userMessage.update("${systemMessage.content}\n\n${userMessage.content}")
        return listOf(combinedUserMessage) + drop(2)
    }

    private fun toVertexMessage(messages: List<ConversationMessage>) =
        messages.mapNotNull {
            when (it) {
                // TODO
                is UserMessage -> if (it.binaryData.isNotEmpty()) {
                    forRole("user").fromMultiModalData(
                        it.content,
                        *it.binaryData.map { map(it.mimeType, it.data) }.toTypedArray(),
                    )
                } else {
                    forRole("user").fromString(it.content)
                }

                is AssistantMessage -> forRole("model").fromString(it.content)
                is SystemMessage -> null // gemini does not support system message
            }
        }
}
