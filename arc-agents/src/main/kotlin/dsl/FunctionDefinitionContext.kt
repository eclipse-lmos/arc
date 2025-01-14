// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl

import org.eclipse.lmos.arc.agents.functions.*

@DslMarker
annotation class FunctionDefinitionContextMarker

@FunctionDefinitionContextMarker
interface FunctionDefinitionContext {

    fun string(
        name: String,
        description: String,
        required: Boolean = true,
        enum: List<String> = emptyList(),
    ): Pair<ParameterSchema, Boolean>

    fun types(vararg params: Pair<ParameterSchema, Boolean>) = params.toList()

    fun function(
        name: String,
        description: String,
        group: String? = null,
        params: List<Pair<ParameterSchema, Boolean>> = emptyList(),
        isSensitive: Boolean = false,
        fn: suspend DSLContext.(List<String?>) -> String,
    )
}

/**
 * Used as an implicit receiver for functions scripts.
 */
class BasicFunctionDefinitionContext(private val beanProvider: BeanProvider) : FunctionDefinitionContext {

    val functions = mutableListOf<LLMFunction>()

    override fun string(name: String, description: String, required: Boolean, enum: List<String>) =
        ParameterSchema(name, description, ParameterType("string"), enum) to required

    override fun types(vararg params: Pair<ParameterSchema, Boolean>) = params.toList()

    override fun function(
        name: String,
        description: String,
        group: String?,
        params: List<Pair<ParameterSchema, Boolean>>,
        isSensitive: Boolean,
        fn: suspend DSLContext.(List<String?>) -> String,
    ) {
        functions.add(
            LambdaLLMFunction(
                name,
                description,
                group,
                isSensitive,
                ParametersSchema(
                    parameters = params.map { it.first },
                    required = params.filter { it.second }.map { it.first.name },
                ),
                BasicDSLContext(beanProvider),
                wrapOutput(fn),
            ),
        )
    }

    /**
     * Wraps the function and adds the BasicScriptingContext#output to the final result if applicable.
     */
    private fun wrapOutput(fn: suspend DSLContext.(List<String?>) -> String): suspend DSLContext.(List<String?>) -> String =
        { args ->
            val result = fn(args)
            if (this is BasicDSLContext) {
                (output.get() + result).trimIndent()
            } else {
                result.trimIndent()
            }
        }
}
