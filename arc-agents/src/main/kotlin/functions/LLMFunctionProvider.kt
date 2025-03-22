// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.functions

import org.eclipse.lmos.arc.agents.FunctionNotFoundException
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.MissingBeanException
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.getOrNull
import org.eclipse.lmos.arc.core.result
import java.io.Closeable
import java.util.*
import kotlin.reflect.KClass

/**
 * Provides LLMFunctions.
 * Usually there is one instance of this class per application.
 */
interface LLMFunctionProvider {

    suspend fun provide(
        functionName: String,
        context: ToolLoaderContext? = null,
    ): Result<LLMFunction, FunctionNotFoundException>

    suspend fun provideAll(context: ToolLoaderContext? = null): List<LLMFunction>
}

/**
 * Loads Functions.
 * Typically, a [LLMFunctionProvider] uses [LLMFunctionLoader]s to load Arc Functions from different sources.
 * There can be many implementations of [LLMFunctionLoader]s in an application.
 */
fun interface LLMFunctionLoader {

    suspend fun load(context: ToolLoaderContext?): List<LLMFunction>
}

/**
 * Implementation of the [LLMFunctionProvider] that combines multiple [LLMFunctionLoader]s and a list of [LLMFunction]s.
 */
class CompositeLLMFunctionProvider(
    private val loaders: List<LLMFunctionLoader>,
    private val functions: List<LLMFunction> = emptyList(),
) : LLMFunctionProvider, Closeable {

    /**
     * Retrieves a list of LLMFunctions matching the given function name.
     *
     * @param functionName The name of the function to search for.
     * @return List of LLMFunctions matching the function name.
     * @throws NoSuchElementException if no matching LLMFunction is found.
     */
    override suspend fun provide(
        functionName: String,
        context: ToolLoaderContext?,
    ): Result<LLMFunction, FunctionNotFoundException> =
        functions(context).firstOrNull { it.name == functionName }?.let { Success(it) }
            ?: Failure(FunctionNotFoundException("No matching LLMFunction found for name: $functionName"))

    override suspend fun provideAll(context: ToolLoaderContext?): List<LLMFunction> = functions(context)

    private suspend fun functions(context: ToolLoaderContext?) = loaders.flatMap { it.load(context) } + functions

    override fun close() {
        loaders.forEach {
            if (it is Closeable) {
                it.close()
            }
        }
    }
}

/**
 * Implementation of the [LLMFunctionLoader] that is backed by a list of [LLMFunction]s.
 */
class ListFunctionsLoader : LLMFunctionLoader {

    private val allFunctions = Vector<LLMFunction>()

    override suspend fun load(context: ToolLoaderContext?): List<LLMFunction> = allFunctions

    fun addAll(functions: List<LLMFunction>) {
        allFunctions.addAll(functions)
    }
}

/**
 * Context for loading functions/tools. This enabled the dynamic loading of functions, for example, based on requests.
 */
interface ToolLoaderContext {

    /**
     * Provides access to Beans in the context.
     * May throw a [MissingBeanException] if the bean is not available.
     * The getOptional() extension function can be used to get a null instead of an exception.
     */
    suspend fun <T : Any> context(type: KClass<T>): T
}

/**
 * Shorthand to access classes from the context.
 */
suspend inline fun <reified T : Any> ToolLoaderContext.get(): T = context(T::class)

/**
 * Returns the requested bean or null if it is not available.
 */
suspend inline fun <reified T : Any> ToolLoaderContext.getOptional() =
    result<T, MissingBeanException> { context(T::class) }.getOrNull()

/**
 * Adapter to convert a [DSLContext] to a [ToolLoaderContext].
 */
fun DSLContext.toToolLoaderContext() = ToolContextAdapter(this)

class ToolContextAdapter(private val context: DSLContext) : ToolLoaderContext {
    override suspend fun <T : Any> context(type: KClass<T>): T = context.context(type)
}
