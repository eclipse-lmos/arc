// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package ai.ancf.lmos.arc.agents.dsl

import ai.ancf.lmos.arc.agents.functions.LLMFunction
import kotlinx.coroutines.future.await
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
