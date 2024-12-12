// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.core

import org.slf4j.LoggerFactory.getLogger
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A Result class that enables the type of error case to be defined.
 * In kotlin, exceptions are meant to be returned as opposed to being thrown.
 * The Result class helps to simplify that approach.
 *
 * (https://elizarov.medium.com/kotlin-and-exceptions-8062f589d07)
 */
sealed class Result<out T, out E : Exception>
data class Success<out T>(val value: T) : Result<T, Nothing>()
data class Failure<out E : Exception>(val reason: E) : Result<Nothing, E>()

/**
 * Returns the value produced by the [block] parameter as a [Result].
 * If an exception is thrown within the [block] that matches the type declared on the [Result],
 * then that exception is returned as the result.
 * All other exceptions will be rethrown.
 */
inline fun <R, reified E : Exception> Any.result(block: ResultBlock<R, E>.() -> R): Result<R, E> {
    val context = BasicResultBlock<R, E>()
    val result = try {
        Success(context.block())
    } catch (ex: Throwable) {
        if (ex is E) {
            Failure(ex)
        } else {
            getLogger(this::class.java).warn("Unexpected error encountered in ${this::class.simpleName}!...", ex)
            throw ex
        }
    }
    context.finallyBlocks.forEach { it(result) }
    return result
}

/**
 * Type safe way of throwing an exception.
 */
fun <R, E : Exception> ResultBlock<R, E>.failWith(block: () -> E): Nothing {
    throw block()
}

/**
 * Defines a block of code that is always called after result block has finished.
 * The block is always executed even on failures caused by undefined Throwables or Exceptions.
 */
@OptIn(ExperimentalContracts::class)
fun <R, E : Exception> ResultBlock<R, E>.finally(block: (Result<R, E>) -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    (this as BasicResultBlock).finallyBlocks.add(block)
}

/**
 * Provides a handy way to close resources.
 * The specified block of code that is always called after result block has finished,
 * even on failures caused by undefined Throwables or Exceptions.
 */
context(ResultBlock<R, E>)
infix fun <R, E : Exception, T> T.closeWith(block: (T) -> Unit): T {
    (this@ResultBlock as BasicResultBlock).finallyBlocks.add { block(this) }
    return this
}

/**
 * Return the value of the success case or throws the exception from the "onFailure" block.
 * This acts as a bridge to the standard [kotlin.Result].
 */
context(ResultBlock<R, E>)
inline infix fun <R, E : Exception> kotlin.Result<R>.failWith(block: (Exception) -> E): R {
    return try {
        getOrThrow()
    } catch (ex: Exception) {
        throw block(ex)
    }
}

/**
 * Return the value of the success case or fail with the exception returned from the "block" block.
 */
context(ResultBlock<R, E>)
inline infix fun <R, E : Exception, R2, E2 : Exception> Result<R2, E2>.failWith(block: (E2) -> E): R2 {
    return when (this) {
        is Success -> value
        is Failure -> throw block(reason)
    }
}

/**
 * If the predicate evaluates to true, then throw the exception form the body.
 * This should usually be called in a "catching" block.
 */
inline fun <R, E : Exception> ResultBlock<R, E>.ensure(predicate: Boolean, block: () -> E) {
    if (!predicate) {
        throw block()
    }
}

/**
 * If the predicate is null, then throw the exception form the body.
 * This should usually be called in a "result" block.
 */
@OptIn(ExperimentalContracts::class)
inline fun <R, E : Exception> ResultBlock<R, E>.ensureNotNull(predicate: Any?, block: () -> E) {
    contract {
        returns() implies (predicate != null)
    }
    if (predicate == null) {
        throw block()
    }
}

/**
 * Calls [block] if the result is a [Failure].
 */
inline infix fun <R, E : Exception> Result<R, E>.onFailure(block: (E) -> Unit): Result<R, E> {
    if (this is Failure) block(reason)
    return this
}

/**
 * Return the value of the success case or throws the exception.
 */
fun <R, E : Exception> Result<R, E>.getOrThrow(): R {
    return when (this) {
        is Success -> value
        is Failure -> throw reason
    }
}

/**
 * Return the value of the success case or null.
 */
fun <R, E : Exception> Result<R, E>.getOrNull(): R? {
    return when (this) {
        is Success -> value
        is Failure -> null
    }
}

/**
 * Creates a new Result with the exception mapped to a new type.
 */
inline fun <R, E : Exception, T : Exception> Result<R, E>.mapFailure(transform: (E) -> T): Result<R, T> {
    return when (this) {
        is Success -> this
        is Failure -> Failure(transform(reason))
    }
}

/**
 * Provides a way to recover from a failure.
 * The transform block is only called if the result is a [Failure].
 */
@OptIn(ExperimentalContracts::class)
inline fun <R, E : Exception> Result<R, E>.recover(transform: (E) -> R?): Result<R, E> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when (this) {
        is Success -> this
        is Failure -> transform(reason)?.let { Success(it) } ?: this
    }
}

/**
 * Creates a new Result with the value mapped to a new type.
 */
inline fun <R, E : Exception, T> Result<R, E>.map(transform: (R) -> T): Result<T, E> {
    return when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }
}

/**
 * Used to enforce that certain functions can only be called with the "result" block.
 */
interface ResultBlock<R, E : Exception>

class BasicResultBlock<R, E : Exception> : ResultBlock<R, E> {

    val finallyBlocks: MutableList<(Result<R, E>) -> Unit> = Collections.synchronizedList(mutableListOf())
}
