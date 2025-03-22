// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.openai

import kotlinx.serialization.json.*
import org.eclipse.lmos.arc.agents.functions.ParameterSchema
import org.eclipse.lmos.arc.agents.functions.ParametersSchema

/**
 * Extension functions for ParametersSchema to convert it to OpenAPI JSON format.
 */
fun ParametersSchema.toOpenAIObject(): JsonObject {
    val propertiesJson = properties.mapValues { (_, parameter) -> parameter.toOpenAIObject() }
    return buildJsonObject {
        put("type", JsonPrimitive(type))
        // Always include "required" as an array (defaults to empty if null)
        put("required", JsonArray((required ?: emptyList()).map { JsonPrimitive(it) }))
        put("properties", JsonObject(propertiesJson))
    }
}

fun ParametersSchema.toOpenAISchemaAsMap() = jsonObjectToMap(toOpenAIObject())

/**
 * Extension functions for ParameterSchema to convert it to OpenAPI JSON format.
 */
private fun ParameterSchema.toOpenAIObject(): JsonObject = buildJsonObject {
    put("type", JsonPrimitive(type))

    description?.takeIf { it.isNotEmpty() }?.let {
        put("description", JsonPrimitive(it))
    }

    // Handle array type with items schema
    items?.let {
        put("items", it.toOpenAIObject())
    }

    // Handle object type with properties
    properties?.takeIf { it.isNotEmpty() }?.let { props ->
        val propertiesJson = props.mapValues { (_, value) -> value.toOpenAIObject() }
        put("properties", JsonObject(propertiesJson))
    }

    // Always include "required" as an array (defaults to empty if null)
    put("required", JsonArray((required ?: emptyList()).map { JsonPrimitive(it) }))

    // Handle enums if available
    enum?.takeIf { it.isNotEmpty() }?.let {
        put("enum", JsonArray(it.map { enumVal -> JsonPrimitive(enumVal) }))
    }
}

/**
 * Utility function to convert a JsonObject to a Map<String, Any?>.
 */
fun jsonObjectToMap(jsonObject: JsonObject): Map<String, Any?> = jsonObject.mapValues { (_, value) ->
    when (value) {
        is JsonPrimitive -> when {
            value.isString -> value.content
            else -> value.booleanOrNull ?: value.intOrNull ?: value.floatOrNull ?: value.doubleOrNull
        }
        is JsonArray -> value.map { jsonElement ->
            when (jsonElement) {
                is JsonObject -> jsonObjectToMap(jsonElement)
                is JsonPrimitive -> when {
                    jsonElement.isString -> jsonElement.content
                    else -> jsonElement.booleanOrNull ?: jsonElement.intOrNull ?: jsonElement.floatOrNull ?: jsonElement.doubleOrNull
                }
                else -> null
            }
        }
        is JsonObject -> jsonObjectToMap(value)
        else -> null
    }
}
