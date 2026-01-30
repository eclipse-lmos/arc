// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.inbound.query

import com.expediagroup.graphql.server.operations.Query
import org.eclipse.lmos.adl.server.services.McpService

class McpToolsQuery(private val mcpService: McpService) : Query {

    suspend fun getMcpTools(): List<AdlTool> {
        return mcpService.getAllTools().map {
             AdlTool(
                 name = it.name,
                 description = it.description,
                 parameters = it.parameters.toString()
             )
        }
    }

    fun mcpServerUrls(): List<String> {
        return mcpService.getMcpServerUrls()
    }
}

data class AdlTool(
    val name: String,
    val description: String,
    val parameters: String
)
