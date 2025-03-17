// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.functions

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Helper functions to convert ParametersSchema to different formats.
 */
fun ParametersSchema.toJsonString() = json.encodeToString(ParametersSchema.serializer(), this)
fun ParametersSchema.toJson() = json.encodeToJsonElement(ParametersSchema.serializer(), this) as JsonObject
fun ParameterSchema.toJson() = json.encodeToJsonElement(ParameterSchema.serializer(), this) as JsonObject

val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    prettyPrint = true
    explicitNulls = false
}

/**
 * Converts a [JsonObject] to a [Map] of [String] to [Any?].
 */
fun ParametersSchema.toJsonMap(): Map<String, Any?> = toJson().toJsonMap()
fun JsonObject.toJsonMap(): Map<String, Any?> = mapValues { (_, value) ->
    when (value) {
        is JsonPrimitive -> {
            when {
                value.isString -> value.content
                else -> value.booleanOrNull ?: value.intOrNull ?: value.floatOrNull ?: value.doubleOrNull
            }
        }

        is JsonArray -> value.map { jsonElement ->
            when (jsonElement) {
                is JsonObject -> jsonElement.toJsonMap()
                is JsonPrimitive -> {
                    when {
                        jsonElement.isString -> jsonElement.content
                        else ->
                            jsonElement.booleanOrNull ?: jsonElement.intOrNull ?: jsonElement.floatOrNull
                            ?: jsonElement.doubleOrNull
                    }
                }

                else -> null
            }
        }

        is JsonObject -> value.toJsonMap()
        else -> null
    }
}
