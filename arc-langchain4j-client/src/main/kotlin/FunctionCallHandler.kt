// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.langchain4j

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.FunctionNotFoundException
import org.eclipse.lmos.arc.agents.HallucinationDetectedException
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.LLMFunctionCalledEvent
import org.eclipse.lmos.arc.agents.functions.convertToJsonMap
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
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
    val functions: List<LLMFunction>,
    private val eventHandler: EventPublisher?,
    private val tracer: AgentTracer?,
    private val functionCallLimit: Int = 60,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val functionCallCount = AtomicInteger(0)

    private val _calledFunctions = ConcurrentHashMap<String, LLMFunction>()
    val calledFunctions get(): Map<String, LLMFunction> = _calledFunctions

    fun calledSensitiveFunction() = _calledFunctions.any { it.value.isSensitive }

    suspend fun handle(assistantMessage: AiMessage) = result<List<ChatMessage>, ArcException> {
        if (functionCallCount.incrementAndGet() > functionCallLimit) {
            failWith {
                ArcException("Function call limit exceeded!")
            }
        }

        // The LLM is requesting the calling of the function we defined in the original request
        if (assistantMessage.hasToolExecutionRequests()) {
            val toolsRequests = assistantMessage.toolExecutionRequests()

            log.debug("Received ${toolsRequests.size} tool calls..")
            val toolMessages = buildList {
                toolsRequests.forEach { toolCall ->
                    val functionName = toolCall.name()
                    val functionArguments = toolCall.arguments().toJson() failWith { it }
                    val function = functions.find { it.name == functionName }
                        ?: failWith { FunctionNotFoundException(functionName) }

                    val functionCallResult: Result<String, ArcException>
                    val duration = measureTime {
                        tracer.spanToolCall { tags, _ ->
                            tags.addToolTags(function, toolCall.arguments())
                            functionCallResult = callFunction(function, functionArguments)
                            tags.addToolOutput(functionCallResult)
                        }
                    }
                    eventHandler?.publish(
                        LLMFunctionCalledEvent(
                            functionName,
                            functionArguments,
                            functionCallResult,
                            duration = duration,
                            version = function.version,
                            description = function.description,
                            outputDescription = function.outputDescription,
                        ),
                    )

                    val toolExecutionResultMessage =
                        ToolExecutionResultMessage.from(toolCall, functionCallResult failWith { it })
                    add(toolExecutionResultMessage)
                }
            }
            listOf(assistantMessage) + toolMessages
        } else {
            emptyList()
        }
    }

    private suspend fun callFunction(function: LLMFunction, functionArguments: Map<String, Any?>) =
        result<String, ArcException> {
            log.debug("Calling LLMFunction $function with $functionArguments...")
            _calledFunctions[function.name] = function
            function.execute(functionArguments) failWith { ArcException(cause = it.cause) }
        }

    private fun String.toJson() = result<Map<String, Any?>, HallucinationDetectedException> {
        convertToJsonMap().getOrNull() ?: failWith {
            HallucinationDetectedException("LLM has failed to produce valid JSON for function call! -> ${this@toJson}")
        }
    }
}
