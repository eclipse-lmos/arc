// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.functions

import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.core.Result

/**
 * Describes a function that can be passed to a Large Language Model.
 */
interface LLMFunction {
    val name: String
    val parameters: ParametersSchema
    val description: String
    val group: String?
    val isSensitive: Boolean

    suspend fun execute(input: Map<String, Any?>): Result<String, LLMFunctionException>
}

/**
 * Exceptions thrown when an LLMFunction fails.
 */
class LLMFunctionException(msg: String, override val cause: Exception? = null) : Exception(msg, cause)

/**
 * Schema tha describes LLM Functions parameters.
 */
@Serializable
data class ParametersSchema(val required: List<String>, val parameters: List<ParameterSchema>)

/**
 * Schema that describes a single parameter of an LLM Function.
 */
@Serializable
data class ParameterSchema(
    val name: String,
    val description: String,
    val type: ParameterType,
    val enum: List<String>,
)

/**
 * A parameter type that can be used by LLM Functions.
 */

@Serializable
class ParameterType(
    val schemaType: String,
    val items: ParameterType? = null,
    val properties: List<ParameterSchema>? = null,
)

/**
 * Extension functions for ParameterSchema to convert it to a JsonSchema Map.
 */
fun ParameterSchema.toSchemaMap(): Pair<String, Map<String, Any>> = name to buildMap {
    put("type", type.schemaType)
    if (description.isNotEmpty()) put("description", description)
    if (enum.isNotEmpty()) put("enum", enum)

    when (type.schemaType) {
        "array" -> {
            val itemsObject = buildMap {
                put("type", type.items?.schemaType ?: "unknown")

                val items = type.items
                if (items?.schemaType == "object") {
                    items.properties?.let { propertiesList ->
                        val properties = propertiesList.associate { it.name to it.toSchemaMap() }
                        put("properties", properties)
                    }
                }
            }
            put("items", itemsObject)
        }

        "object" -> {
            type.properties?.let { props ->
                val properties = props.associate { it.name to it.toSchemaMap() }
                put("properties", properties)
            }
        }
    }
}

fun List<ParameterSchema>.toSchemaMap(): Map<String, Map<String, Any>> = associate { it.toSchemaMap() }
