// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.graphql.inbound

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.agents.functions.LLMFunctionProvider
import org.eclipse.lmos.arc.agents.functions.toJsonMap
import org.eclipse.lmos.arc.core.getOrThrow

/**
 * Executes a tool.
 */
class ToolMutation(private val functionProvider: LLMFunctionProvider) : Mutation {

    @GraphQLDescription("Executes a tool with the given name and parameters.")
    suspend fun tool(toolExecution: ToolExecution): String {
        val params = toolExecution.parameters.toJsonMap()
        return functionProvider.provide(toolExecution.name).getOrThrow().execute(params).getOrThrow()
    }
}

@Serializable
data class ToolExecution(
    val name: String,
    val parameters: String,
)
