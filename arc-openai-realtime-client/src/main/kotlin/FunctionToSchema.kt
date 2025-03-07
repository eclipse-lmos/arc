// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agent.client.ws

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.toJson

/**
 * Converts an LLMFunction to a JSON Schema.
 */
fun LLMFunction.toJsonSchema(): JsonObject {
    return buildJsonObject {
        put("type", JsonPrimitive("function"))
        put("name", JsonPrimitive(name))
        put("description", JsonPrimitive(description))
        put("parameters", parameters.toJson())
    }
}
