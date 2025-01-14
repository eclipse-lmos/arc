// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.functions

import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.mapFailure
import org.eclipse.lmos.arc.core.result
import org.slf4j.LoggerFactory

/**
 * Bridges a kotlin function to the LLMFunction interface.
 */
data class LambdaLLMFunction(
    override val name: String,
    override val description: String,
    override val group: String?,
    override val isSensitive: Boolean,
    override val parameters: ParametersSchema,
    private val context: DSLContext,
    private val function: suspend DSLContext.(List<String?>) -> String,
) : LLMFunction, FunctionWithContext {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Executes the function and returns the result.
     */
    override suspend fun execute(input: Map<String, Any?>): Result<String, LLMFunctionException> {
        return try {
            log.debug("Calling function $name with $input")
            result<String, Exception> {
                function.invoke(context, parameters.parameters.map { p -> input[p.name]?.let { "$it" } })
            }.mapFailure { LLMFunctionException("LLMFunction call $name failed! ", it) }
        } catch (ex: Exception) {
            Failure(LLMFunctionException("LLMFunction call $name failed!", ex))
        }
    }

    override fun withContext(context: DSLContext): LLMFunction {
        return LambdaLLMFunction(name, description, group, isSensitive, parameters, context, function)
    }
}
