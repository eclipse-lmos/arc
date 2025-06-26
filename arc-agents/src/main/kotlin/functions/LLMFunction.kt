// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.functions

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.eclipse.lmos.arc.core.Result

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

    suspend fun execute(input: Map<String, Any?>): Result<String, LLMFunctionException>
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
    val type: String,
    @Transient val name: String? = null,
    val description: String? = null,
    val items: ParameterSchema? = null,
    val properties: Map<String, ParameterSchema>? = null,
    val required: List<String>? = null,
    @Transient val isRequired: Boolean = true,
    val enum: List<String>? = null,
)
