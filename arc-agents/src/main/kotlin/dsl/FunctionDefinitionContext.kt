// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl

import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.LambdaLLMFunction
import org.eclipse.lmos.arc.agents.functions.ParameterSchema
import org.eclipse.lmos.arc.agents.functions.ParametersSchema

@DslMarker
annotation class FunctionDefinitionContextMarker

@FunctionDefinitionContextMarker
interface FunctionDefinitionContext {

    fun string(name: String, description: String, required: Boolean = true, enum: List<String>? = null): ParameterSchema

    fun objectType(
        name: String,
        description: String,
        properties: List<ParameterSchema>,
        required: Boolean = true,
    ): ParameterSchema

    fun integer(name: String, description: String, required: Boolean = true): ParameterSchema

    fun boolean(name: String, description: String, required: Boolean = true): ParameterSchema

    fun number(name: String, description: String, required: Boolean = true): ParameterSchema

    fun array(
        name: String,
        description: String,
        itemType: String = "string",
        required: Boolean = true,
        enum: List<String>? = null,
    ): ParameterSchema

    fun types(vararg params: ParameterSchema) = ParametersSchema(
        properties = params.associateBy { it.name ?: error("Name required for parameter $it!") },
        required = params.filter { it.isRequired }.map { it.name ?: error("Name required for parameter $it!") }
            .toList(),
    )

    fun function(
        name: String,
        description: String,
        group: String? = null,
        params: ParametersSchema = ParametersSchema(),
        isSensitive: Boolean = false,
        fn: suspend DSLContext.(List<Any?>) -> String,
    )
}

/**
 * Used as an implicit receiver for functions scripts.
 */
class BasicFunctionDefinitionContext(private val beanProvider: BeanProvider) : FunctionDefinitionContext {

    val functions = mutableListOf<LLMFunction>()

    override fun string(name: String, description: String, required: Boolean, enum: List<String>?) =
        ParameterSchema("string", name, description, enum = enum, isRequired = required)

    override fun integer(name: String, description: String, required: Boolean) =
        ParameterSchema("integer", name, description, isRequired = required)

    override fun boolean(name: String, description: String, required: Boolean) =
        ParameterSchema("boolean", name, description, isRequired = required)

    override fun number(name: String, description: String, required: Boolean) =
        ParameterSchema("number", name, description, isRequired = required)

    override fun array(name: String, description: String, itemType: String, required: Boolean, enum: List<String>?) =
        ParameterSchema(
            "array",
            name,
            description,
            items = ParameterSchema(itemType),
            isRequired = required,
            enum = enum,
        )

    override fun objectType(
        name: String,
        description: String,
        properties: List<ParameterSchema>,
        required: Boolean,
    ) =
        ParameterSchema(
            "object",
            name,
            description,
            isRequired = required,
            properties = properties.associateBy { it.name ?: error("Name required for parameter $it!") },
            required = properties.filter { it.isRequired }.map { it.name ?: error("Name required for parameter $it!") }
                .toList(),
        )

    override fun function(
        name: String,
        description: String,
        group: String?,
        params: ParametersSchema,
        isSensitive: Boolean,
        fn: suspend DSLContext.(List<Any?>) -> String,
    ) {
        functions.add(
            LambdaLLMFunction(
                name,
                description,
                group,
                isSensitive,
                params,
                BasicDSLContext(beanProvider),
                wrapOutput(fn),
            ),
        )
    }

    /**
     * Wraps the function and adds the BasicScriptingContext#output to the final result if applicable.
     */
    private fun wrapOutput(fn: suspend DSLContext.(List<Any?>) -> String): suspend DSLContext.(List<Any?>) -> String =
        { args ->
            val result = fn(args)
            if (this is BasicDSLContext) {
                (output.get() + result).trimIndent()
            } else {
                result.trimIndent()
            }
        }
}

/**
 * Helper functions for creating parameter schemas.
 */
fun string(name: String, description: String, required: Boolean = true, enum: List<String>? = null) =
    ParameterSchema("string", name, description, enum = enum, isRequired = required)

fun integer(name: String, description: String, required: Boolean = true) =
    ParameterSchema("integer", name, description, isRequired = required)

fun boolean(name: String, description: String, required: Boolean = true) =
    ParameterSchema("boolean", name, description, isRequired = required)

fun number(name: String, description: String, required: Boolean = true) =
    ParameterSchema("number", name, description, isRequired = required)

fun array(name: String, description: String, itemType: String, required: Boolean = true) =
    ParameterSchema(
        "array",
        name,
        description,
        items = ParameterSchema(itemType),
        isRequired = required,
    )

fun objectType(
    name: String,
    description: String,
    properties: List<ParameterSchema>,
    required: Boolean = true,
) =
    ParameterSchema(
        "object",
        name,
        description,
        isRequired = required,
        properties = properties.associateBy { it.name ?: error("Name required for parameter $it!") },
        required = properties.filter { it.isRequired }.map { it.name ?: error("Name required for parameter $it!") }
            .toList(),
    )

fun types(vararg params: ParameterSchema) = ParametersSchema(
    properties = params.associateBy { it.name ?: error("Name required for parameter $it!") },
    required = params.filter { it.isRequired }.map { it.name ?: error("Name required for parameter $it!") }
        .toList(),
)
