// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.search.inbound

import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

/**
 * Global exception handler for GraphQL data fetcher exceptions.
 * Catches exceptions thrown during GraphQL query execution and converts them
 * into appropriate GraphQL errors.
 */
class GlobalExceptionHandler : DataFetcherExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    override fun handleException(
        handlerParameters: DataFetcherExceptionHandlerParameters?,
    ): CompletableFuture<DataFetcherExceptionHandlerResult?> {
        if (handlerParameters == null) {
            return CompletableFuture.completedFuture(DataFetcherExceptionHandlerResult.newResult().build())
        }

        val exception = handlerParameters.exception
        val environment = handlerParameters.dataFetchingEnvironment
        val path = handlerParameters.path

        log.error(
            "Exception while fetching data for field '{}' at path '{}': {}",
            environment.field?.name,
            path,
            exception.message,
            exception,
        )

        val error: GraphQLError = GraphqlErrorBuilder.newError()
            .message(sanitizeErrorMessage(exception))
            .path(path)
            .location(environment.field?.sourceLocation)
            .build()

        val result = DataFetcherExceptionHandlerResult.newResult()
            .error(error)
            .build()

        return CompletableFuture.completedFuture(result)
    }

    /**
     * Sanitizes the error message to avoid exposing sensitive internal details.
     * Override this method to customize error message handling.
     */
    private fun sanitizeErrorMessage(exception: Throwable): String {
        return when (exception) {
            is IllegalArgumentException -> exception.message ?: "Invalid argument"
            is IllegalStateException -> exception.message ?: "Invalid state"
            is NoSuchElementException -> exception.message ?: "Resource not found"
            else -> "An internal error occurred"
        }
    }
}
