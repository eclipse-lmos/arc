// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.graphql.inbound

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.agents.functions.LLMFunctionProvider
import org.eclipse.lmos.arc.agents.functions.toJsonMap
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.getOrThrow

/**
 * Executes a tool.
 */
class ToolMutation(private val functionProvider: LLMFunctionProvider) : Mutation {

    @GraphQLDescription("Executes a tool with the given name and parameters.")
    suspend fun tool(name: String, parameters: String): ToolExecutionResult {
        val params = parameters.toJsonMap()
        val result = functionProvider.provide(name).getOrThrow().execute(params)
        return when (result) {
            is Success -> ToolExecutionResult(result = result.value)
            is Failure -> ToolExecutionResult(error = result.reason.message ?: "Unknown error")
            else -> ToolExecutionResult(error = "Unknown error")
        }
    }
}

@Serializable
data class ToolExecutionResult(val result: String? = null, val error: String? = null)
