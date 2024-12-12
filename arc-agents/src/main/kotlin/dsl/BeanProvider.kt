// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl

import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.isPresent
import kotlinx.coroutines.withContext
import org.eclipse.lmos.arc.core.getOrNull
import org.eclipse.lmos.arc.core.result
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.reflect.KClass

interface BeanProvider {

    suspend fun <T : Any> provide(bean: KClass<T>): T
}

suspend inline fun <reified T : Any> BeanProvider.provideOptional() =
    result<T, MissingBeanException> { provide(T::class) }.getOrNull()

/**
 * Provides beans stored in the current Kotlin Coroutine Context.
 * Optional, it will use the BeanProvider as a fallback.
 */
class CoroutineBeanProvider(private val fallbackBeanProvider: BeanProvider? = null) : BeanProvider {

    companion object {
        private val contextLocal = ThreadLocal.withInitial { emptySet<Any>() }
    }

    override suspend fun <T : Any> provide(type: KClass<T>): T {
        if (!contextLocal.isPresent()) return fallbackOrThrow(type)
        return contextLocal.get()?.firstOrNull { type.isInstance(it) }?.let { it as T }
            ?: fallbackOrThrow(type)
    }

    private suspend fun <T : Any> fallbackOrThrow(type: KClass<T>) =
        fallbackBeanProvider?.provide(type) ?: throw MissingBeanException("Bean of type $type cannot be located!")

    /**
     * Sets the context for the current Coroutine context.
     */
    @OptIn(ExperimentalContracts::class)
    suspend fun <T> setContext(contextObj: Set<Any>, fn: suspend () -> T): T {
        contract {
            callsInPlace(fn, EXACTLY_ONCE)
        }
        return withContext(contextLocal.asContextElement(value = contextLocal.get() + contextObj)) {
            try {
                fn()
            } finally {
                contextLocal.remove()
            }
        }
    }
}

/**
 * BeanProvider backed by a Set of beans.
 */
class SetBeanProvider(private val beans: Set<Any>) : BeanProvider {

    override suspend fun <T : Any> provide(bean: KClass<T>): T {
        return beans.firstOrNull { bean.isInstance(it) }?.let { it as T } ?: fallbackOrThrow(bean)
    }

    private fun <T : Any> fallbackOrThrow(type: KClass<T>): Nothing =
        throw MissingBeanException("Bean of type $type cannot be located!")
}

/**
 * Creates a SetBeanProvider with the provided beans.
 */
fun beans(vararg beans: Any) = SetBeanProvider(beans.toSet())

/**
 * Composite BeanProvider
 */
class CompositeBeanProvider(
    private val beans: Set<Any>,
    private val fallbackBeanProvider: BeanProvider? = null,
) : BeanProvider {

    override suspend fun <T : Any> provide(bean: KClass<T>): T {
        return beans.firstOrNull { bean.isInstance(it) }?.let { it as T } ?: fallbackOrThrow(bean)
    }

    private suspend fun <T : Any> fallbackOrThrow(type: KClass<T>) =
        fallbackBeanProvider?.provide(type) ?: throw MissingBeanException("Bean of type $type cannot be located!")
}

class MissingBeanException(message: String) : Exception(message)
