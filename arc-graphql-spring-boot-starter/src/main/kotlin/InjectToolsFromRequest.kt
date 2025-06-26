// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.graphql

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
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
import org.eclipse.lmos.arc.agents.functions.ToolLoaderContext
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
 * A provider for LLM functions that are passed in the agent request.
 *
 * This class implements the LLMFunctionProvider interface and is responsible for:
 * 1. Extracting function definitions from the agent request's system context
 * 2. Parsing these definitions from JSON format
 * 3. Creating dynamic LLMFunction implementations based on the parsed definitions
 * 4. Delegating to another function provider for additional functions not defined in the request
 *
 * Functions in the request are expected to be in JSON format with the following structure:
 * ```
 * {
 *   "name": "functionName",
 *   "description": "Function description",
 *   "parameters": [
 *     {
 *       "name": "paramName",
 *       "description": "Parameter description",
 *       "type": "paramType"
 *     }
 *   ],
 *   "value": "returnValue"
 * }
 * ```
 *
 * @param request The agent request containing function definitions in its system context
 * @param functionProvider A delegate provider for additional functions not defined in the request
 */
class RequestFunctionProvider(private val request: AgentRequest, private val functionProvider: LLMFunctionProvider) :
    LLMFunctionProvider {

    private val log = LoggerFactory.getLogger(this.javaClass)

    /**
     * Provides a specific LLM function by name.
     *
     * @param functionName The name of the function to provide
     * @param context Optional context for loading tools
     * @return A Result containing either the requested function or a FunctionNotFoundException
     */
    override suspend fun provide(functionName: String, context: ToolLoaderContext?) =
        result<LLMFunction, FunctionNotFoundException> {
            provideAll().firstOrNull { it.name == functionName }
                ?: failWith { FunctionNotFoundException(functionName) }
        }

    /**
     * Provides all available LLM functions by combining functions from the request and the delegate provider.
     *
     * This method:
     * 1. Extracts function definitions from the request's system context (keys starting with "function")
     * 2. Parses each function from JSON format
     * 3. Creates dynamic LLMFunction implementations
     * 4. Combines these with functions from the delegate provider
     *
     * @param context Optional context for loading tools (not used in this implementation)
     * @return A list of all available LLM functions
     */
    override suspend fun provideAll(context: ToolLoaderContext?): List<LLMFunction> {
        val requestFunctions = mutableSetOf<String>()

        return request.systemContext.filter { it.key.startsWith("function") }.mapNotNull {
            log.info("Loading Function from Request: ${it.key}")
            try {
                val fn = Json.parseToJsonElement(it.value).jsonObject
                val parameters = fn["parameters"]?.takeIf { it is JsonArray }?.jsonArray?.map {
                    ParameterSchema(
                        name = it.jsonObject["name"]!!.jsonPrimitive.content,
                        description = it.jsonObject["description"]!!.jsonPrimitive.content,
                        type = it.jsonObject["type"]!!.jsonPrimitive.content,
                    )
                } ?: emptyList()

                requestFunctions.add(fn["name"]!!.jsonPrimitive.content)

                object : LLMFunction {
                    override val name: String = fn["name"]!!.jsonPrimitive.content
                    override val version: String? = null
                    override val description: String = fn["description"]!!.jsonPrimitive.content
                    override val group: String? = null
                    override val isSensitive: Boolean = false
                    override val outputDescription: String? = null
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
        } + functionProvider.provideAll(context).filter { !requestFunctions.contains(it.name) }
    }
}
