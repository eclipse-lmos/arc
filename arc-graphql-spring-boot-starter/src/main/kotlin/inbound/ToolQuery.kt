// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.graphql.inbound

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.agents.functions.LLMFunctionProvider
import org.eclipse.lmos.arc.agents.functions.toJson

/**
 * Returns the list of available tools (functions) provided by the LLMFunctionProvider.
 */
class ToolQuery(private val functionProvider: LLMFunctionProvider) : Query {

    @GraphQLDescription("Returns the list of available tools.")
    suspend fun tool(): Tools {
        return Tools(
            functionProvider.provideAll().map { function ->
                Tool(
                    name = function.name,
                    description = function.description,
                    parameters = function.parameters.toJson().toString(),
                )
            },
        )
    }
}

@Serializable
data class Tools(
    val tools: List<Tool>,
)

@Serializable
data class Tool(
    val name: String,
    val description: String,
    val parameters: String,
)
