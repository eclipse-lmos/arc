// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl

import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.core.getOrNull
import org.eclipse.lmos.arc.core.result
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass

@DslMarker
annotation class SBaseContextMarker

/**
 * Implicit receiver for the body of functions.
 */
@SBaseContextMarker
interface DSLContext {

    /**
     * Provides access to Beans in the context.
     * May throw a [MissingBeanException] if the bean is not available.
     * The getOptional() extension function can be used to get a null instead of an exception.
     */
    suspend fun <T : Any> context(type: KClass<T>): T

    operator fun String.unaryPlus()

    /**
     * Sets a local value that is only available during the current request.
     */
    fun setLocal(key: String, value: Any?)

    /**
     * Gets a local value that is only available during the current request.
     */
    fun getLocal(key: String): Any?
}

/**
 * Shorthand to access classes from the context.
 */
suspend inline fun <reified T : Any> DSLContext.get(): T = context(T::class)

class BasicDSLContext(private val beanProvider: BeanProvider) : DSLContext {

    val functions = mutableListOf<LLMFunction>()

    val output = AtomicReference("")

    private val localMap = ConcurrentHashMap<String, Any>()

    override fun setLocal(key: String, value: Any?) {
        if (value == null) {
            localMap.remove(key)
            return
        }
        localMap[key] = value
    }

    override fun getLocal(key: String): Any? = localMap[key]

    override fun String.unaryPlus() {
        output.updateAndGet { it + this }
    }

    override suspend fun <T : Any> context(type: KClass<T>) = beanProvider.provide(type)
}

/**
 * Returns the requested bean or null if it is not available.
 */
suspend inline fun <reified T : Any> DSLContext.getOptional() =
    result<T, MissingBeanException> { this@getOptional.context(T::class) }.getOrNull()

/**
 * Used to run functions in a DSL context with a set of beans.
 */
suspend fun withDSLContext(beans: Set<Any> = emptySet(), block: suspend DSLContext.() -> Unit) {
    BasicDSLContext(SetBeanProvider(beans)).block()
}

/**
 * A version of the DSLContext that can be used in the tools section of the agent definition.
 * This will override the + operator for strings to build a list of tools instead of an output string.
 */
class ToolsDSLContext(private val context: DSLContext) : DSLContext by context {

    val tools = mutableListOf<String>()

    override operator fun String.unaryPlus() {
        tools.add(this)
    }
}
