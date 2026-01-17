// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound.mutation

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import org.eclipse.lmos.adl.server.embeddings.UseCaseEmbeddingsStore
import org.eclipse.lmos.adl.server.model.Adl
import org.eclipse.lmos.adl.server.storage.AdlStorage

/**
 * GraphQL Mutation for storing UseCases in the Embeddings store.
 */
class AdlMutation(
    private val useCaseStore: UseCaseEmbeddingsStore,
    private val adlStorage: AdlStorage,
) : Mutation {

    @GraphQLDescription("Stores a UseCase in the embeddings store. Embeddings are generated from the provided examples.")
    suspend fun store(
        @GraphQLDescription("Unique identifier for the ADL") id: String,
        @GraphQLDescription("The content of the ADL") content: String,
        @GraphQLDescription("Tags associated with the ADL") tags: List<String>,
        @GraphQLDescription("Timestamp when the ADL was created") createdAt: String
    ): StorageResult {
        adlStorage.store(Adl(id, content, tags, createdAt))
        val storedCount = useCaseStore.storeUseCase(content)
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
