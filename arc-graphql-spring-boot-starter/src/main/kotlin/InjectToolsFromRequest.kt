// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.graphql

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.eclipse.lmos.arc.agents.AgentFailedException
import org.eclipse.lmos.arc.agents.FunctionNotFoundException
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.LLMFunctionException
import org.eclipse.lmos.arc.agents.functions.LLMFunctionProvider
import org.eclipse.lmos.arc.agents.functions.ParameterSchema
import org.eclipse.lmos.arc.agents.functions.ParametersSchema
import org.eclipse.lmos.arc.api.AgentRequest
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.failWith
import org.eclipse.lmos.arc.core.result
import org.slf4j.LoggerFactory

/**
 * Enable the injection of tools from a request. The Arc View can pass tools to the agent in the request.
 * This ContextHandler will inject those tools into the agent execution context.
 */
class InjectToolsFromRequest(private val functionProvider: LLMFunctionProvider) : ContextHandler {

    override suspend fun inject(
        request: AgentRequest,
        block: suspend (Set<Any>) -> Result<Conversation, AgentFailedException>,
    ): Result<Conversation, AgentFailedException> {
        return block(setOf(RequestFunctionProvider(request, functionProvider)))
    }
}

/**
 * A provider for functions that are passed in the request.
 */
class RequestFunctionProvider(private val request: AgentRequest, private val functionProvider: LLMFunctionProvider) :
    LLMFunctionProvider {

    private val log = LoggerFactory.getLogger(this.javaClass)

    override suspend fun provide(functionName: String) = result<LLMFunction, FunctionNotFoundException> {
        provideAll().firstOrNull { it.name == functionName }
            ?: failWith { FunctionNotFoundException(functionName) }
    }

    override suspend fun provideAll(): List<LLMFunction> {
        return request.systemContext.filter { it.key.startsWith("function") }.mapNotNull {
            log.info("Loading Function from Request: ${it.key}")
            try {
                val fn = Json.parseToJsonElement(it.value).jsonObject
                val parameters = fn["parameters"]?.jsonArray?.map {
                    ParameterSchema(
                        name = it.jsonObject["name"]!!.jsonPrimitive.content,
                        description = it.jsonObject["description"]!!.jsonPrimitive.content,
                        type = it.jsonObject["type"]!!.jsonPrimitive.content,
                    )
                } ?: emptyList()
                object : LLMFunction {
                    override val name: String = fn["name"]!!.jsonPrimitive.content
                    override val description: String = fn["description"]!!.jsonPrimitive.content
                    override val group: String? = null
                    override val isSensitive: Boolean = false
                    override val parameters: ParametersSchema =
                        ParametersSchema(properties = parameters.associateBy { it.name!! })

                    override suspend fun execute(input: Map<String, Any?>) = result<String, LLMFunctionException> {
                        fn["value"]!!.jsonPrimitive.content
                    }
                }
            } catch (e: Exception) {
                log.error("Error parsing function: ${it.key}", e)
                null
            }
        } + functionProvider.provideAll()
    }
}
