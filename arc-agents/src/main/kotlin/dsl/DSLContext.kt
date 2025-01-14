// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl

import kotlinx.coroutines.future.await
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass

@DslMarker
annotation class SBaseContextMarker

/**
 * Implicit receiver for the body of functions.
 */
@SBaseContextMarker
interface DSLContext {

    suspend fun <T : Any> context(type: KClass<T>): T

    suspend fun httpGet(url: String): String

    operator fun String.unaryPlus()
}

/**
 * Shorthand to access classes from the context.
 */
suspend inline fun <reified T : Any> DSLContext.get(): T = context(T::class)

class BasicDSLContext(private val beanProvider: BeanProvider) : DSLContext {

    val functions = mutableListOf<LLMFunction>()

    val output = AtomicReference("")

    override fun String.unaryPlus() {
        output.updateAndGet { it + this }
    }

    override suspend fun <T : Any> context(type: KClass<T>) = beanProvider.provide(type)

    override suspend fun httpGet(url: String): String {
        val request = HttpRequest.newBuilder().uri(URI(url)).GET().build()
        val response = HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        return response.body()
    }
}

/**
 * Used to run functions in a DSL context with a set of beans.
 */
fun withDSLContext(beans: Set<Any> = emptySet(), block: DSLContext.() -> Unit) {
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
