// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.openai

import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.FunctionNotFoundException
import org.eclipse.lmos.arc.agents.HallucinationDetectedException
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.LLMFunctionCalledEvent
import org.eclipse.lmos.arc.agents.functions.LLMFunctionStartedEvent
import org.eclipse.lmos.arc.agents.functions.convertToJsonMap
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.eclipse.lmos.arc.agents.tracing.Tags
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.failWith
import org.eclipse.lmos.arc.core.getOrNull
import org.eclipse.lmos.arc.core.result
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.measureTime

/**
 * Finds function calls in ChatCompletions and calls the callback function if any are found.
 */
class FunctionCallHandler(
    val functions: List<LLMFunction>,
    private val eventHandler: EventPublisher?,
    private val functionCallLimit: Int = 30,
    private val tracer: AgentTracer,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val functionCallCount = AtomicInteger(0)

    private val _calledFunctions = ConcurrentHashMap<String, ToolCall>()
    val calledFunctions get(): Map<String, ToolCall> = _calledFunctions

    fun calledSensitiveFunction() = _calledFunctions.any { it.value.function.isSensitive }

    suspend fun handle(chatCompletions: ChatCompletions) = result<List<ChatRequestMessage>, ArcException> {
        val choice = chatCompletions.choices[0]

        if (functionCallCount.incrementAndGet() > functionCallLimit) {
            failWith {
                ArcException("Function call limit exceeded!")
            }
        }

        // The LLM is requesting the calling of the function we defined in the original request
        // There seems to be a bug where the toolCalls are defined, but the finishReason is not set to TOOL_CALLS.
        if (CompletionsFinishReason.TOOL_CALLS == choice.finishReason || choice.message?.toolCalls?.isNotEmpty() == true) {
            val assistantMessage = ChatRequestAssistantMessage("")
            assistantMessage.setToolCalls(choice.message.toolCalls)

            log.debug("Received ${choice.message.toolCalls.size} tool calls..")
            val toolMessages = buildList {
                choice.message.toolCalls.forEach {
                    val toolCall = it as ChatCompletionsFunctionToolCall
                    val functionName = toolCall.function.name
                    val functionArguments = toolCall.function.arguments.toJson() failWith { it }

                    val functionCallResult: Result<String, ArcException>
                    val functionHolder = AtomicReference<LLMFunction?>()
                    val duration = measureTime {
                        eventHandler?.publish(LLMFunctionStartedEvent(functionName, functionArguments))
                        functionCallResult = tracer.withSpan("tool") { tags, _ ->
                            OpenInferenceTags.applyToolAttributes(functionName, toolCall, tags)
                            callFunction(functionName, functionArguments, tags, functionHolder, toolCall).also {
                                OpenInferenceTags.applyToolAttributes(it, tags)
                            }
                        }
                    }
                    eventHandler?.publish(
                        LLMFunctionCalledEvent(
                            functionName,
                            functionArguments,
                            functionCallResult,
                            duration = duration,
                            version = functionHolder.get()?.version,
                            description = functionHolder.get()?.description,
                            outputDescription = functionHolder.get()?.outputDescription,
                        ),
                    )

                    add(
                        ChatCompletionMessageParam.ofTool(
                            ChatCompletionToolMessageParam.builder()
                                .content(functionCallResult failWith { it })
                                .toolCallId(toolCall.id())
                                .role(JsonValue.from("tool"))
                                .build(),
                        ),
                    )
                }
            }
            listOf(assistantMessage) + toolMessages
        } else {
            emptyList()
        }
    }

    private suspend fun callFunction(
        functionName: String,
        functionArguments: Map<String, Any?>,
        tags: Tags,
        functionHolder: AtomicReference<LLMFunction?>,
        toolCall: ChatCompletionsFunctionToolCall,
    ) =
        result<String, ArcException> {
            val function = functions.find { it.name == functionName } ?: failWith {
                tags.error(FunctionNotFoundException(functionName))
                FunctionNotFoundException(functionName)
            }
            functionHolder.set(function) // TODO this is not nice

            log.debug("Calling LLMFunction $function with $functionArguments...")
            _calledFunctions[functionName] = ToolCall(functionName, function, toolCall.function.arguments)
            OpenInferenceTags.applyToolAttributes(function, tags)
            function.execute(functionArguments) failWith {
                tags.error(it)
                ArcException(cause = it.cause)
            }
        }

    private fun String.toJson() = result<Map<String, Any?>, HallucinationDetectedException> {
        convertToJsonMap().getOrNull() ?: failWith {
            HallucinationDetectedException("LLM has failed to produce valid JSON for function call! -> ${this@toJson}")
        }
    }
}

data class ToolCall(val name: String, val function: LLMFunction, val arguments: String)
