// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package ai.ancf.lmos.arc.client.azure

import ai.ancf.lmos.arc.agents.ArcException
import ai.ancf.lmos.arc.agents.HallucinationDetectedException
import ai.ancf.lmos.arc.agents.events.EventPublisher
import ai.ancf.lmos.arc.agents.functions.LLMFunction
import ai.ancf.lmos.arc.agents.functions.LLMFunctionCalledEvent
import ai.ancf.lmos.arc.agents.functions.LLMFunctionStartedEvent
import ai.ancf.lmos.arc.agents.functions.convertToJsonMap
import ai.ancf.lmos.arc.core.Result
import ai.ancf.lmos.arc.core.failWith
import ai.ancf.lmos.arc.core.getOrNull
import ai.ancf.lmos.arc.core.result
import com.azure.ai.openai.models.ChatCompletions
import com.azure.ai.openai.models.ChatCompletionsFunctionToolCall
import com.azure.ai.openai.models.ChatRequestAssistantMessage
import com.azure.ai.openai.models.ChatRequestMessage
import com.azure.ai.openai.models.ChatRequestToolMessage
import com.azure.ai.openai.models.CompletionsFinishReason
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

    suspend fun handle(chatCompletions: ChatCompletions) = result<List<ChatRequestMessage>, ArcException> {
        val choice = chatCompletions.choices[0]

        if (functionCallCount.incrementAndGet() > functionCallLimit) {
            failWith {
                ArcException("Function call limit exceeded!")
            }
        }

        // The LLM is requesting the calling of the function we defined in the original request
        if (CompletionsFinishReason.TOOL_CALLS == choice.finishReason) {
            val assistantMessage = ChatRequestAssistantMessage("")
            assistantMessage.setToolCalls(choice.message.toolCalls)

            log.debug("Received ${choice.message.toolCalls.size} tool calls..")
            val toolMessages = buildList {
                choice.message.toolCalls.forEach {
                    val toolCall = it as ChatCompletionsFunctionToolCall
                    val functionName = toolCall.function.name
                    val functionArguments = toolCall.function.arguments.toJson() failWith { it }

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

                    add(ChatRequestToolMessage(functionCallResult failWith { it }, toolCall.id))
                }
            }
            listOf(assistantMessage) + toolMessages
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
