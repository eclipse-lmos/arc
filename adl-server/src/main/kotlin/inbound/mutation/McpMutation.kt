// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.inbound.mutation

import com.expediagroup.graphql.server.operations.Mutation
import org.eclipse.lmos.adl.server.services.McpService

class McpMutation(private val mcpService: McpService) : Mutation {

    fun setMcpServerUrls(urls: List<String>): Boolean {
        mcpService.setMcpServerUrls(urls)
        return true
    }
}
