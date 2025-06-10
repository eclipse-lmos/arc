// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.graphql.inbound

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.agents.dsl.BasicDSLContext
import org.eclipse.lmos.arc.agents.dsl.BeanProvider
import org.eclipse.lmos.arc.agents.functions.FunctionWithContext
import org.eclipse.lmos.arc.agents.functions.LLMFunctionProvider
import org.eclipse.lmos.arc.agents.functions.toJsonMap
import org.eclipse.lmos.arc.agents.functions.toToolLoaderContext
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.getOrThrow

/**
 * Executes a tool.
 */
class ToolMutation(private val functionProvider: LLMFunctionProvider, private val beans: BeanProvider) : Mutation {

    @GraphQLDescription("Executes a tool with the given name and parameters.")
    suspend fun tool(name: String, parameters: String?): ToolExecutionResult {
        val params = parameters?.takeIf { it.isNotBlank() }?.let { parameters.toJsonMap() } ?: emptyMap()
        val context = BasicDSLContext(beans)
        val tool = functionProvider.provide(name, context.toToolLoaderContext()).getOrThrow()

        val result = if (tool is FunctionWithContext) {
            tool.withContext(context).execute(params)
        } else {
            tool.execute(params)
        }

        return when (result) {
            is Success -> ToolExecutionResult(result = result.value)
            is Failure -> ToolExecutionResult(error = result.reason.message ?: "Unknown error")
            else -> ToolExecutionResult(error = "Unknown error")
        }
    }
}

@Serializable
data class ToolExecutionResult(val result: String? = null, val error: String? = null)
