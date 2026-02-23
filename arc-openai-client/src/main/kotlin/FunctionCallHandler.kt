// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.openai

import com.openai.core.JsonValue
import com.openai.models.chat.completions.ChatCompletion
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam
import com.openai.models.chat.completions.ChatCompletionMessageParam
import com.openai.models.chat.completions.ChatCompletionToolMessageParam
import com.openai.models.chat.completions.ChatCompletionMessageToolCall
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
import org.eclipse.lmos.arc.core.getOrThrow
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

    suspend fun handle(chatCompletions: ChatCompletion) = result<List<ChatCompletionMessageParam>, ArcException> {
        val choice = chatCompletions.choices().first()

        if (functionCallCount.incrementAndGet() > functionCallLimit) {
            failWith {
                ArcException("Function call limit exceeded!")
            }
        }

        // The LLM is requesting the calling of the function we defined in the original request
        // There seems to be a bug where the toolCalls are defined, but the finishReason is not set to TOOL_CALLS.
        val finishReason: String? = choice.finishReason()?.toString()
        if ((finishReason == "tool_calls" || finishReason?.contains("tool_calls") == true) || choice.message().toolCalls().isPresent) {
            val assistantMessage = ChatCompletionAssistantMessageParam.builder()
                .role(JsonValue.from("assistant"))
                .toolCalls(choice.message().toolCalls().get())
                .build()

            log.debug("Received ${choice.message().toolCalls().get().size} tool calls..")
            val toolMessages = buildList {
                choice.message().toolCalls().get().forEach {
                    val toolCall = it
                    val functionName = toolCall.function().name()
                    val functionArgumentsStr = toolCall.function().arguments() // arguments() returns String in Stainless SDK
                    val functionArguments = functionArgumentsStr.toJson().getOrThrow()

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
                                .build(),
                        ),
                    )
                }
            }
            listOf(ChatCompletionMessageParam.ofAssistant(assistantMessage)) + toolMessages
        } else {
            emptyList()
        }
    }

    private suspend fun callFunction(
        functionName: String,
        functionArguments: Map<String, Any?>,
        tags: Tags,
        functionHolder: AtomicReference<LLMFunction?>,
        toolCall: ChatCompletionMessageToolCall,
    ) =
        result<String, ArcException> {
            val function = functions.find { it.name == functionName } ?: failWith {
                tags.error(FunctionNotFoundException(functionName))
                FunctionNotFoundException(functionName)
            }
            functionHolder.set(function) // TODO this is not nice

            log.debug("Calling LLMFunction $function with $functionArguments...")
            _calledFunctions[functionName] = ToolCall(functionName, function, toolCall.function().arguments())
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
