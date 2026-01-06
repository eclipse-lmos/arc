// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import org.eclipse.lmos.adl.server.embeddings.QdrantUseCaseEmbeddingsStore
import org.eclipse.lmos.adl.server.embeddings.UseCaseSearchResult

/**
 * GraphQL Query for searching UseCases based on conversation embeddings.
 */
class AdlQuery(
    private val useCaseStore: QdrantUseCaseEmbeddingsStore,
) : Query {

    @GraphQLDescription("Returns the supported version of the ALD.")
    fun version(): String {
        return "1.0.0"
    }

    @GraphQLDescription("Returns the UseCase IDs that match the conversation provided, ordered by relevance.")
    suspend fun search(
        conversation: List<Message>,
        @GraphQLDescription("Maximum number of results to return") limit: Int? = null,
        @GraphQLDescription("Minimum similarity score (0.0 to 1.0)") scoreThreshold: Double? = 0.0,
    ): List<UseCaseMatch> {
        if (limit != null) require(limit in 1..100) { "limit must be between 1 and 100" }
        if (scoreThreshold != null) require(scoreThreshold in 0.0f..1.0f) { "scoreThreshold must be between 0.0 and 1.0" }
        require(conversation.isNotEmpty()) { "conversation must not be empty" }
        val results = useCaseStore.searchByConversation(conversation, limit ?: 10, scoreThreshold?.toFloat() ?: 0.0f)
        return results.toMatches()
    }

    @GraphQLDescription("Searches for UseCases using a text query.")
    suspend fun searchByText(
        query: String,
        @GraphQLDescription("Maximum number of results to return") limit: Int? = null,
        @GraphQLDescription("Minimum similarity score (0.0 to 1.0)") scoreThreshold: Double? = 0.0,
    ): List<UseCaseMatch> {
        if (limit != null) require(limit in 1..100) { "limit must be between 1 and 100" }
        if (scoreThreshold != null) require(scoreThreshold in 0.0f..1.0f) { "scoreThreshold must be between 0.0 and 1.0" }
        require(query.isNotBlank()) { "query must not be blank" }
        val results = useCaseStore.search(query, limit ?: 10, scoreThreshold?.toFloat() ?: 0.0f)
        return results.toMatches()
    }

    private fun List<UseCaseSearchResult>.toMatches(): List<UseCaseMatch> {
        return groupBy { it.useCaseId }
            .map { (useCaseId, matches) ->
                UseCaseMatch(
                    maxScore = matches.maxOf { it.score },
                    useCaseId = useCaseId,
                    matchedExamples = matches.map { Example(it.score, it.example) },
                    content = matches.first().content,
                )
            }
            .sortedByDescending { it.maxScore }
    }
}

/**
 * Represents a matched UseCase with its relevance score.
 */
@GraphQLDescription("A UseCase match result with relevance score")
data class UseCaseMatch(
    @param:GraphQLDescription("The ID of the matched UseCase")
    val useCaseId: String,
    @param:GraphQLDescription("The UseCase content")
    val content: String,
    @param:GraphQLDescription("The max similarity score (0.0 to 1.0)")
    val maxScore: Float,
    @param:GraphQLDescription("The examples that matched the query")
    val matchedExamples: List<Example>,
)

@GraphQLDescription("A UseCase match result with relevance score")
data class Example(
    @param:GraphQLDescription("The similarity score (0.0 to 1.0)")
    val score: Float,
    @param:GraphQLDescription("The examples that matched the query")
    val example: String,
)
