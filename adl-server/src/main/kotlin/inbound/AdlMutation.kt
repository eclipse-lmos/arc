// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import org.eclipse.lmos.adl.server.embeddings.UseCaseEmbeddingsStore

/**
 * GraphQL Mutation for storing UseCases in the Embeddings store.
 */
class AdlMutation(
    private val useCaseStore: UseCaseEmbeddingsStore,
) : Mutation {

    @GraphQLDescription("Stores a UseCase in the embeddings store. Embeddings are generated from the provided examples.")
    suspend fun store(adl: String): StorageResult {
        val storedCount = useCaseStore.storeUseCase(adl)
        return StorageResult(
            storedExamplesCount = storedCount,
            message = "UseCase successfully stored with $storedCount embeddings",
        )
    }

    @GraphQLDescription("Deletes a UseCase from the embeddings store.")
    suspend fun delete(
        @GraphQLDescription("The unique ID of the UseCase to delete") useCaseId: String,
    ): DeletionResult {
        useCaseStore.deleteByUseCaseId(useCaseId)
        return DeletionResult(
            useCaseId = useCaseId,
            message = "UseCase successfully deleted",
        )
    }

    @GraphQLDescription("Clears all UseCases from the embeddings store.")
    suspend fun clearAll(): ClearResult {
        useCaseStore.clear()
        return ClearResult(
            message = "All UseCases successfully cleared",
        )
    }
}

/**
 * Result of a UseCase storage operation.
 */
@GraphQLDescription("Result of storing a UseCase")
data class StorageResult(
    @param:GraphQLDescription("Number of example embeddings that were stored")
    val storedExamplesCount: Int,
    @param:GraphQLDescription("A message describing the result")
    val message: String,
)

/**
 * Result of a UseCase deletion operation.
 */
@GraphQLDescription("Result of deleting a UseCase")
data class DeletionResult(
    @param:GraphQLDescription("The ID of the UseCase")
    val useCaseId: String,
    @param:GraphQLDescription("A message describing the result")
    val message: String,
)

/**
 * Result of clearing all UseCases.
 */
@GraphQLDescription("Result of clearing all UseCases")
data class ClearResult(
    @param:GraphQLDescription("A message describing the result")
    val message: String,
)
