// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound.mutation

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import org.eclipse.lmos.adl.server.model.Adl
import org.eclipse.lmos.adl.server.repositories.AdlRepository
import org.eclipse.lmos.adl.server.repositories.UseCaseEmbeddingsRepository
import org.eclipse.lmos.arc.assistants.support.usecases.toUseCases
import org.slf4j.LoggerFactory
import java.time.Instant.now

/**
 * GraphQL Mutation for storing UseCases in the Embeddings store.
 */
class AdlStorageMutation(
    private val useCaseStore: UseCaseEmbeddingsRepository,
    private val adlStorage: AdlRepository,
) : Mutation {

    private val log = LoggerFactory.getLogger(this::class.java)

    @GraphQLDescription("Stores a UseCase in the embeddings store. Embeddings are generated from the provided examples.")
    suspend fun store(
        @GraphQLDescription("Unique identifier for the ADL") id: String,
        @GraphQLDescription("The content of the ADL") content: String,
        @GraphQLDescription("Tags associated with the ADL") tags: List<String>,
        @GraphQLDescription("Timestamp when the ADL was created") createdAt: String? = null,
        @GraphQLDescription("Examples") examples: List<String>,
    ): StorageResult {
        log.info("Storing ADL with id: {} with {} examples", id, examples.size)
        val allExamples = content.toUseCases().flatMap { it.examples.split("\n") }.filter { it.isNotBlank() } + examples
        adlStorage.store(Adl(id, content.trim(), tags, createdAt ?: now().toString(), allExamples))
        val storedCount = useCaseStore.storeUtterances(id, allExamples)
        log.debug("Successfully stored ADL with id: {}. Generated {} embeddings.", id, storedCount)
        return StorageResult(
            storedExamplesCount = storedCount,
            message = "UseCase successfully stored with $storedCount embeddings",
        )
    }

    @GraphQLDescription("Updates the tags of an existing ADL.")
    suspend fun updateTags(
        @GraphQLDescription("The unique ID of the ADL") id: String,
        @GraphQLDescription("The new list of tags") tags: List<String>,
    ): StorageResult {
        log.info("Updating tags for ADL with id: {}", id)
        val existingAdl = adlStorage.get(id) ?: throw IllegalArgumentException("ADL with id $id not found")

        val updatedAdl = existingAdl.copy(tags = tags)
        adlStorage.store(updatedAdl)

        return StorageResult(
            storedExamplesCount = existingAdl.examples.size,
            message = "Tags successfully updated",
        )
    }

    @GraphQLDescription("Deletes a UseCase from the embeddings store.")
    suspend fun delete(
        @GraphQLDescription("The unique ID of the UseCase to delete") id: String,
    ): DeletionResult {
        log.info("Deleting ADL with id: {}", id)
        adlStorage.deleteById(id)
        useCaseStore.deleteByUseCaseId(id)
        return DeletionResult(
            useCaseId = id,
            message = "UseCase successfully deleted",
        )
    }

    @GraphQLDescription("Clears all UseCases from the embeddings store.")
    suspend fun clearAll(): ClearResult {
        log.info("Clearing all ADLs from store")
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
