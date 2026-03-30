// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.functions

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import org.eclipse.lmos.arc.core.Result
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Describes a function that can be passed to a Large Language Model.
 */
interface LLMFunction {
    val name: String

    val version: String?
    val parameters: ParametersSchema
    val description: String
    val group: String?
    val isSensitive: Boolean
    val outputDescription: String?
    val metadata: Map<String, Any> get() = emptyMap()

    suspend fun execute(input: Map<String, Any?>): Result<Any, LLMFunctionException>
}

/**
 * Json Parser to convert Tool responses to a json string.
 */
private val jsonToString = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    prettyPrint = true
    isLenient = true
}

/**
 * Converts any object to a string that can be returned as a tool result.
 */
private val log: Logger = LoggerFactory.getLogger(LLMFunction::class.java)
fun Any?.toStringResult(): String {
    if (this == null) return ""
    if (this is String) return this.trim()
    try {
        return jsonToString.encodeToString(this)
    } catch (e: Exception) {
        log.warn("Unable to convert $this to Json!", e)
        return this.toString()
    }
}

/**
 * Exceptions thrown when an LLMFunction fails.
 */
class LLMFunctionException(msg: String, override val cause: Exception? = null) : Exception(msg, cause)

/**
 * Schema that describes a single parameter of an LLM Function.
 */
@Serializable
data class ParametersSchema(
    val type: String = "object",
    val properties: Map<String, ParameterSchema> = emptyMap(),
    val required: List<String>? = null,
)

/**
 * Schema that describes a single parameter of an LLM Function.
 */
@Serializable
data class ParameterSchema(
    val type: String?,
    @Transient val name: String? = null,
    val description: String? = null,
    val items: ParameterSchema? = null,
    val properties: Map<String, ParameterSchema>? = null,
    val required: List<String>? = null,
    @Transient val isRequired: Boolean = true,
    val enum: List<String>? = null,
    val anyOf: List<ParameterSchema>? = null,
    val maxLength: Int? = null,
    val minLength: Int? = null,
    val multipleOf: Int? = null,
    val minimum: Int? = null,
    val maximum: Int? = null,
)
