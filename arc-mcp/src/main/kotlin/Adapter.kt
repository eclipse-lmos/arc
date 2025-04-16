// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.mcp

import io.modelcontextprotocol.spec.McpSchema.Tool
import org.eclipse.lmos.arc.agents.functions.ParameterSchema
import org.eclipse.lmos.arc.agents.functions.ParametersSchema

/**
 * Converts a [Tool] JsonSchema to a [ParametersSchema].
 */
fun parameters(tool: Tool): ParametersSchema {
    return ParametersSchema(
        required = tool.inputSchema.required,
        type = tool.inputSchema.type,
        properties = tool.inputSchema.properties.mapValues { (_, v) ->
            val type = v as Map<String, Any>
            mapProperties(type)
        },
    )
}

private fun mapProperties(type: Map<String, Any>): ParameterSchema {
    return ParameterSchema(
        type = type["type"] as String,
        description = type["description"] as String?,
        name = type["name"] as String?,
        required = type["required"] as List<String>?,
        items = (type["items"] as Map<String, Any>?)?.let { ParameterSchema(type = it["type"].toString()) },
        enum = (type["enum"] as List<Any>?)?.map { it.toString() },
        properties = (type["properties"] as Map<String, Any>?)?.mapValues { (_, v) ->
            val type = v as Map<String, Any>
            mapProperties(type)
        },
    )
}
