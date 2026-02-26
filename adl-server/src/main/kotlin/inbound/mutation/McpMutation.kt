// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.inbound.mutation

import com.expediagroup.graphql.server.operations.Mutation
import org.eclipse.lmos.adl.server.models.McpServerDetails
import org.eclipse.lmos.adl.server.services.McpService

/**
 * GraphQL mutation for setting MCP server URLs.
 */
class McpMutation(private val mcpService: McpService) : Mutation {

    suspend fun setMcpServerUrls(urls: List<String>): List<McpServerDetails> {
        return mcpService.setMcpServerUrls(urls)
    }
}
