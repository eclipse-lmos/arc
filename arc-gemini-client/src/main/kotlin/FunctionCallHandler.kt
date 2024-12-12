// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.gemini

import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.LLMFunctionCalledEvent
import org.eclipse.lmos.arc.agents.functions.LLMFunctionStartedEvent
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.failWith
import org.eclipse.lmos.arc.core.result
import com.google.cloud.vertexai.api.Content
import com.google.cloud.vertexai.api.FunctionCall
import com.google.cloud.vertexai.api.GenerateContentResponse
import com.google.cloud.vertexai.generativeai.ContentMaker.fromMultiModalData
import com.google.cloud.vertexai.generativeai.PartMaker.fromFunctionResponse
import com.google.cloud.vertexai.generativeai.ResponseHandler
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.measureTime

/**
 * Finds function calls in ChatCompletions and calls the callback function if any are found.
 */
class FunctionCallHandler(private val functions: List<LLMFunction>, private val eventHandler: EventPublisher?) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val _calledFunctions = ConcurrentHashMap<String, LLMFunction>()

    val calledFunctions get(): Map<String, LLMFunction> = _calledFunctions

    fun calledSensitiveFunction() = _calledFunctions.any { it.value.isSensitive }

    suspend fun handle(response: GenerateContentResponse) = result<List<Content>, ArcException> {
        val functionCalls = ResponseHandler.getFunctionCalls(response)

        log.debug("Received ${functionCalls.size} tool calls..")
        if (functionCalls.isNotEmpty()) {
            buildList {
                add(ResponseHandler.getContent(response)) // add function_call response from model
                functionCalls.forEach { functionCall ->
                    val functionArguments = functionCall.args.fieldsMap.mapValues { it.value.stringValue }
                    val functionCallResult: Result<String, ArcException>
                    val duration = measureTime {
                        eventHandler?.publish(LLMFunctionStartedEvent(functionCall.name, functionArguments))
                        functionCallResult = callFunction(functionCall.name, functionArguments)
                    }
                    eventHandler?.publish(
                        LLMFunctionCalledEvent(
                            functionCall.name,
                            functionArguments,
                            functionCallResult,
                            duration = duration,
                        ),
                    )
                    // add function responses
                    add(functionCall.toResponse(functionCallResult failWith { it }))
                }
            }
        } else {
            emptyList()
        }
    }

    private fun FunctionCall.toResponse(response: String) =
        fromMultiModalData(fromFunctionResponse(name, mapOf(name to response))) // TODO check this

    private suspend fun callFunction(functionName: String, functionArguments: Map<String, String>) =
        result<String, ArcException> {
            val function = functions.find { it.name == functionName }
                ?: failWith { ArcException("Cannot find function called $functionName!") }

            log.debug("Calling LLMFunction $function with $functionArguments...")
            _calledFunctions[functionName] = function
            function.execute(functionArguments) failWith { ArcException(cause = it.cause) }
        }
}
