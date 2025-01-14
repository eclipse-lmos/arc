// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.functions

import org.eclipse.lmos.arc.agents.FunctionNotFoundException
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.Success
import java.util.*

/**
 * Provides LLMFunctions.
 * Usually there is one instance of this class per application.
 */
interface LLMFunctionProvider {

    fun provide(functionName: String): Result<LLMFunction, FunctionNotFoundException>

    fun provideAll(): List<LLMFunction>
}

/**
 * Loads Functions.
 * Typically, a [LLMFunctionProvider] uses [LLMFunctionLoader]s to load Arc Functions from different sources.
 * There can be many implementations of [LLMFunctionLoader]s in an application.
 */
fun interface LLMFunctionLoader {

    fun load(): List<LLMFunction>
}

/**
 * Implementation of the [LLMFunctionProvider] that combines multiple [LLMFunctionLoader]s and a list of [LLMFunction]s.
 */
class CompositeLLMFunctionProvider(
    private val loaders: List<LLMFunctionLoader>,
    private val functions: List<LLMFunction> = emptyList(),
) : LLMFunctionProvider {

    /**
     * Retrieves a list of LLMFunctions matching the given function name.
     *
     * @param functionName The name of the function to search for.
     * @return List of LLMFunctions matching the function name.
     * @throws NoSuchElementException if no matching LLMFunction is found.
     */

    override fun provide(functionName: String): Result<LLMFunction, FunctionNotFoundException> =
        functions().firstOrNull { it.name == functionName }?.let { Success(it) }
            ?: Failure(FunctionNotFoundException("No matching LLMFunction found for name: $functionName"))

    override fun provideAll(): List<LLMFunction> = functions()

    private fun functions() = loaders.flatMap { it.load() } + functions
}

/**
 * Implementation of the [LLMFunctionLoader] that is backed by a list of [LLMFunction]s.
 */
class ListFunctionsLoader : LLMFunctionLoader {

    private val allFunctions = Vector<LLMFunction>()

    override fun load(): List<LLMFunction> = allFunctions

    fun addAll(functions: List<LLMFunction>) {
        allFunctions.addAll(functions)
    }
}
