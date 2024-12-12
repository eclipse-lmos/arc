// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring

import org.eclipse.lmos.arc.agents.dsl.BasicFunctionDefinitionContext
import org.eclipse.lmos.arc.agents.dsl.BeanProvider
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.ParameterSchema

/**
 * A factory for creating llm functions using the Arc Agent DSL.
 * Mainly used for creating Functions inside of Configuration classes.
 *
 * For example:
 * @Bean
 *     open fun myFunction(function: Functions) = function(
 *         name = "get_weather",
 *         description = "Returns real-time weather information for any location",
 *     ) {
 *     """
 *         The weather is good in Berlin. It is 20 degrees celsius.
 *     """
 * }
 */
class Functions(private val beanProvider: BeanProvider) {

    operator fun invoke(
        name: String,
        description: String,
        group: String? = null,
        params: List<Pair<ParameterSchema, Boolean>> = emptyList(),
        isSensitive: Boolean = false,
        fn: suspend DSLContext.(List<String?>) -> String,
    ): List<LLMFunction> {
        val context = BasicFunctionDefinitionContext(beanProvider)
        context.function(name, description, group, params, isSensitive, fn)
        return context.functions
    }
}
