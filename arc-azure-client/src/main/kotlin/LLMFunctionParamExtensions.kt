// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.azure

import kotlinx.serialization.json.*
import org.eclipse.lmos.arc.agents.functions.ParameterSchema
import org.eclipse.lmos.arc.agents.functions.ParametersSchema

/**
 * Extension functions for ParametersSchema to convert it to OpenAPI JSON format.
 */
fun ParametersSchema.toAzureOpenAIObject(): JsonObject {
    val properties = parameters.associate { it.name to it.toAzureOpenAIObject() as JsonElement }
    return buildJsonObject {
        put("type", JsonPrimitive("object"))
        put("required", JsonArray(required.map { JsonPrimitive(it) }))
        put("properties", JsonObject(properties))
    }
}

fun ParametersSchema.toAzureOpenAISchemaAsMap() = jsonObjectToMap(toAzureOpenAIObject())

/**
 * Extension functions for ParameterSchema to convert it to OpenAPI JSON format.
 */
private fun ParameterSchema.toAzureOpenAIObject(): JsonObject = buildJsonObject {
    put("type", JsonPrimitive(type.schemaType))

    if (description.isNotEmpty()) {
        put("description", JsonPrimitive(description))
    }

    when (type.schemaType) {
        "array" -> {
            val itemsObject = buildJsonObject {
                put("type", JsonPrimitive(type.items?.schemaType ?: "unknown"))

                val items = type.items
                if (items?.schemaType == "object") {
                    items.properties?.let { propertiesList ->
                        val properties = propertiesList.associate { it.name to it.toAzureOpenAIObject() }
                        put("properties", JsonObject(properties))
                    }
                }
            }
            put("items", itemsObject)
        }

        "object" -> {
            type.properties?.let { props ->
                val properties = props.associate { it.name to it.toAzureOpenAIObject() }
                put("properties", JsonObject(properties))
            }
        }
    }

    if (enum.isNotEmpty()) {
        put("enum", JsonArray(enum.map { JsonPrimitive(it) }))
    }
}

fun jsonObjectToMap(jsonObject: JsonObject): Map<String, Any?> = jsonObject.mapValues { (_, value) ->
    when (value) {
        is JsonPrimitive -> {
            when {
                value.isString -> value.content
                else -> value.booleanOrNull ?: value.intOrNull ?: value.floatOrNull ?: value.doubleOrNull
            }
        }

        is JsonArray -> value.map { jsonElement ->
            when (jsonElement) {
                is JsonObject -> jsonObjectToMap(jsonElement)
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

        is JsonObject -> jsonObjectToMap(value)
        else -> null
    }
}
