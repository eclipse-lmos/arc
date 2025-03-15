// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.openai

import com.openai.core.JsonValue
import com.openai.models.chat.completions.ChatCompletion
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam
import com.openai.models.chat.completions.ChatCompletionMessageParam
import com.openai.models.chat.completions.ChatCompletionToolMessageParam
import com.openai.models.responses.Response
import com.openai.models.responses.ResponseFunctionToolCall
import com.openai.models.responses.ResponseInputItem
import com.openai.models.responses.ResponseInputItem.FunctionCallOutput
import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.HallucinationDetectedException
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.LLMFunctionCalledEvent
import org.eclipse.lmos.arc.agents.functions.LLMFunctionStartedEvent
import org.eclipse.lmos.arc.agents.functions.convertToJsonMap
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.failWith
import org.eclipse.lmos.arc.core.getOrNull
import org.eclipse.lmos.arc.core.result
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.measureTime

/**
 * Finds function calls in ChatCompletions and calls the callback function if any are found.
 */
class FunctionCallHandler(
    private val functions: List<LLMFunction>,
    private val eventHandler: EventPublisher?,
    private val functionCallLimit: Int = 60,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val functionCallCount = AtomicInteger(0)

    private val _calledFunctions = ConcurrentHashMap<String, LLMFunction>()
    val calledFunctions get(): Map<String, LLMFunction> = _calledFunctions

    fun calledSensitiveFunction() = _calledFunctions.any { it.value.isSensitive }

    suspend fun handle(chatCompletions: ChatCompletion) = result<List<ChatCompletionMessageParam>, ArcException> {
        val choice = chatCompletions.choices()[0]

        if (functionCallCount.incrementAndGet() > functionCallLimit) {
            failWith {
                ArcException("Function call limit exceeded!")
            }
        }

        // The LLM is requesting the calling of the function we defined in the original request
        if (ChatCompletion.Choice.FinishReason.TOOL_CALLS == choice.finishReason()) {
            val message = choice.message()
            val assistantMessage = ChatCompletionMessageParam.ofAssistant(
                ChatCompletionAssistantMessageParam.builder()
                    .role(JsonValue.from("assistant"))
                    .toolCalls(message.toolCalls().get())
                    .build(),
            )

            log.debug("Received ${message.toolCalls().get().size} tool calls..")
            val toolMessages = buildList {
                message.toolCalls().get().forEach {
                    val toolCall = it
                    val functionName = toolCall.function().name()
                    val functionArguments = toolCall.function().arguments().toJson() failWith { it }

                    val functionCallResult: Result<String, ArcException>
                    val duration = measureTime {
                        eventHandler?.publish(LLMFunctionStartedEvent(functionName, functionArguments))
                        functionCallResult = callFunction(functionName, functionArguments)
                    }
                    eventHandler?.publish(
                        LLMFunctionCalledEvent(
                            functionName,
                            functionArguments,
                            functionCallResult,
                            duration = duration,
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

    suspend fun handleResponse(responses: Response) = result<List<ResponseInputItem>, ArcException> {
        if (functionCallCount.incrementAndGet() > functionCallLimit) {
            failWith {
                ArcException("Function call limit exceeded!")
            }
        }

        if (responses.output().size > 1) {
            failWith {
                ArcException("More than one function tool call is not supported.")
            }
        }

        val responseOutputItem = responses.output()[0]
        if (responseOutputItem.isFunctionCall()) {
            val functionToolCallItem = responseOutputItem.functionCall().get()
            val functionName = functionToolCallItem.name()
            val functionArguments = functionToolCallItem.arguments().toJson() failWith { it }
            val functionCallResult: Result<String, ArcException>

            log.debug("Received ${functionToolCallItem.name()} tool calls..")

            val duration = measureTime {
                eventHandler?.publish(LLMFunctionStartedEvent(functionName, functionArguments))
                functionCallResult = callFunction(functionName, functionArguments)
            }

            eventHandler?.publish(
                LLMFunctionCalledEvent(
                    functionName,
                    functionArguments,
                    functionCallResult,
                    duration = duration,
                ),
            )

            val functionToolCall = ResponseFunctionToolCall.builder().callId(functionToolCallItem.callId())
                .name(functionName).arguments(functionToolCallItem.arguments()).build()

            val functionToolCallOutput = FunctionCallOutput.builder().callId(functionToolCallItem.callId())
                .output(functionCallResult failWith { it }).build()
            listOf(
                ResponseInputItem.ofFunctionCall(functionToolCall),
                ResponseInputItem.ofFunctionCallOutput(functionToolCallOutput)
            )
        } else {
            emptyList()
        }
    }

    private suspend fun callFunction(functionName: String, functionArguments: Map<String, Any?>) =
        result<String, ArcException> {
            val function = functions.find { it.name == functionName }
                ?: failWith { ArcException("Cannot find function called $functionName!") }

            log.debug("Calling LLMFunction $function with $functionArguments...")
            _calledFunctions[functionName] = function
            function.execute(functionArguments) failWith { ArcException(cause = it.cause) }
        }

    private fun String.toJson() = result<Map<String, Any?>, HallucinationDetectedException> {
        convertToJsonMap().getOrNull() ?: failWith {
            HallucinationDetectedException("LLM has failed to produce valid JSON for function call! -> ${this@toJson}")
        }
    }
}
