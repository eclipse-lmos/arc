// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.repositories.impl

import dev.langchain4j.model.embedding.EmbeddingModel
import io.qdrant.client.PointIdFactory.id
import io.qdrant.client.QdrantClient
import io.qdrant.client.QdrantGrpcClient
import io.qdrant.client.ValueFactory.value
import io.qdrant.client.VectorsFactory.vectors
import io.qdrant.client.grpc.Collections.Distance
import io.qdrant.client.grpc.Collections.VectorParams
import io.qdrant.client.grpc.Points.PointStruct
import io.qdrant.client.grpc.Points.ScoredPoint
import kotlinx.coroutines.guava.await
import org.eclipse.lmos.adl.server.QdrantConfig
import org.eclipse.lmos.adl.server.models.SimpleMessage
import org.eclipse.lmos.adl.server.repositories.UseCaseEmbeddingsRepository
import org.eclipse.lmos.adl.server.repositories.SearchResult
import org.eclipse.lmos.arc.assistants.support.usecases.toUseCases
import java.util.UUID
import java.util.concurrent.ExecutionException

/**
 * A store for UseCase embeddings using Qdrant vector database.
 * Creates embeddings from UseCase examples and stores them in Qdrant for similarity search.
 *
 * @param embeddingModel The embedding model to use for creating embeddings.
 * @param config The Qdrant configuration.
 */
class QdrantUseCaseEmbeddingsStore(
    private val embeddingModel: EmbeddingModel,
    private val config: QdrantConfig = QdrantConfig(),
) : AutoCloseable, UseCaseEmbeddingsRepository {

    private val client: QdrantClient by lazy {
        QdrantClient(
            QdrantGrpcClient.newBuilder(config.host, config.port, false).build(),
        )
    }

    /**
     * Initializes the Qdrant collection if it doesn't exist.
     */
    override suspend fun initialize() {
        try {
            val collections = client.listCollectionsAsync().await()
            if (!collections.contains(config.collectionName)) {
                client.createCollectionAsync(
                    config.collectionName,
                    VectorParams.newBuilder()
                        .setDistance(Distance.Cosine)
                        .setSize(config.vectorSize.toLong())
                        .build(),
                ).get()
            }
        } catch (e: ExecutionException) {
            throw RuntimeException("Failed to initialize Qdrant collection", e.cause)
        }
    }

    /**
     * Stores embeddings the given ADL utterances.
     * Overwrites any existing embeddings for UseCases with the same ID.
     * @param id The ADL identifier.
     * @return The number of embeddings stored.
     */
    override suspend fun storeUtterances(id: String, examples: List<String>): Int {
        val points = mutableListOf<PointStruct>()

        // Delete existing embeddings for all ADL IDs that will be stored
        deleteByUseCaseId(id)

        examples.forEach { example ->
            val embedding = embeddingModel.embed(example).content().vector()
            val point = PointStruct.newBuilder()
                .setId(id(UUID.randomUUID()))
                .setVectors(vectors(embedding.toList()))
                .putAllPayload(buildPayload(id, "", example))
                .build()
            points.add(point)
        }

        if (points.isNotEmpty()) {
            try {
                client.upsertAsync(config.collectionName, points).await()
            } catch (e: ExecutionException) {
                throw RuntimeException("Failed to store embeddings in Qdrant", e.cause)
            }
        }

        return points.size
    }

    /**
     * Stores embeddings the given UseCases.
     * Overwrites any existing embeddings for UseCases with the same ID.
     * @param adl The UseCases to create embeddings from.
     * @return The number of embeddings stored.
     */
    override suspend fun storeUseCase(adl: String, examples: List<String>): Int {
        val points = mutableListOf<PointStruct>()
        val parsedUseCases = adl.toUseCases()

        // Delete existing embeddings for all UseCase IDs that will be stored
        parsedUseCases.forEach { useCase -> deleteByUseCaseId(useCase.id) }

        parsedUseCases.forEach { useCase ->
            val examples = (parseExamples(useCase.examples) + examples).distinct()
            examples.forEach { example ->
                val embedding = embeddingModel.embed(example).content().vector()
                val point = PointStruct.newBuilder()
                    .setId(id(UUID.randomUUID()))
                    .setVectors(vectors(embedding.toList()))
                    .putAllPayload(buildPayload(useCase.id, adl, example))
                    .build()
                points.add(point)
            }
        }

        if (points.isNotEmpty()) {
            try {
                client.upsertAsync(config.collectionName, points).await()
            } catch (e: ExecutionException) {
                throw RuntimeException("Failed to store embeddings in Qdrant", e.cause)
            }
        }

        return points.size
    }

    /**
     * Searches for similar UseCases based on the query text.
     * @param query The query text to search for.
     * @param limit The maximum number of results to return.
     * @param scoreThreshold The minimum similarity score (0.0 to 1.0).
     * @return List of matching UseCase embeddings with their scores.
     */
    override suspend fun search(query: String, limit: Int, scoreThreshold: Float): List<SearchResult> {
        val queryEmbedding = embeddingModel.embed(query).content().vector()
        return searchByVector(queryEmbedding.toList(), limit, scoreThreshold)
    }

    /**
     * Searches for similar UseCases based on an embedding vector.
     * @param embedding The embedding vector to search with.
     * @param limit The maximum number of results to return.
     * @param scoreThreshold The minimum similarity score (0.0 to 1.0).
     * @return List of matching UseCase embeddings with their scores.
     */
    suspend fun searchByVector(
        embedding: List<Float>,
        limit: Int = 5,
        scoreThreshold: Float = 0.0f,
    ): List<SearchResult> {
        return try {
            val searchRequest = io.qdrant.client.grpc.Points.SearchPoints.newBuilder()
                .setCollectionName(config.collectionName)
                .addAllVector(embedding)
                .setLimit(limit.toLong())
                .setScoreThreshold(scoreThreshold)
                .setWithPayload(io.qdrant.client.WithPayloadSelectorFactory.enable(true))
                .build()

            val results = client.searchAsync(searchRequest).await()

            results.map { point: ScoredPoint -> point.toSearchResult() }
        } catch (e: ExecutionException) {
            throw RuntimeException("Failed to search embeddings in Qdrant", e.cause)
        }
    }

    /**
     * Searches for similar UseCases using a conversation (list of messages).
     * Combines all message contents for the search query.
     * @param messages The messages to use for searching.
     * @param limit The maximum number of results to return.
     * @param scoreThreshold The minimum similarity score (0.0 to 1.0).
     * @return List of matching UseCase embeddings with their scores.
     */
    override suspend fun searchByConversation(
        messages: List<SimpleMessage>,
        limit: Int,
        scoreThreshold: Float,
    ): List<SearchResult> {
        val combinedQuery = messages.filter { it.role == "user" && it.content.length > 5 }.takeLast(5).flatMap {
            search(it.content, limit, scoreThreshold)
        }
        return combinedQuery
    }

    /**
     * Deletes all embeddings for a specific UseCase.
     * @param useCaseId The ID of the UseCase to delete embeddings for.
     */
    override suspend fun deleteByUseCaseId(useCaseId: String) {
        try {
            client.deleteAsync(
                config.collectionName,
                io.qdrant.client.grpc.Points.Filter.newBuilder()
                    .addMust(
                        io.qdrant.client.grpc.Points.Condition.newBuilder()
                            .setField(
                                io.qdrant.client.grpc.Points.FieldCondition.newBuilder()
                                    .setKey(PAYLOAD_ADL_ID)
                                    .setMatch(
                                        io.qdrant.client.grpc.Points.Match.newBuilder()
                                            .setKeyword(useCaseId),
                                    ),
                            ),
                    )
                    .build(),
            ).await()
        } catch (e: ExecutionException) {
            throw RuntimeException("Failed to delete embeddings from Qdrant", e.cause)
        }
    }

    /**
     * Clears all embeddings from the collection.
     */
    override suspend fun clear() {
        try {
            client.deleteCollectionAsync(config.collectionName).await()
            initialize()
        } catch (e: ExecutionException) {
            throw RuntimeException("Failed to clear Qdrant collection", e.cause)
        }
    }

    /**
     * Gets the total number of embeddings stored.
     */
    override suspend fun count(): Long {
        return try {
            client.countAsync(config.collectionName).await()
        } catch (e: ExecutionException) {
            throw RuntimeException("Failed to count embeddings in Qdrant", e.cause)
        }
    }

    override fun close() {
        client.close()
    }

    private fun parseExamples(examples: String): List<String> {
        return examples.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.removePrefix("-").trim() }
            .filter { it.isNotEmpty() }
    }

    private fun buildPayload(
        useCaseId: String,
        useCase: String,
        example: String,
    ): Map<String, io.qdrant.client.grpc.JsonWithInt.Value> {
        return buildMap {
            put(PAYLOAD_ADL_ID, value(useCaseId))
            put(PAYLOAD_EXAMPLE, value(example))
            put(PAYLOAD_CONTENT, value(useCase))
        }
    }

    private fun ScoredPoint.toSearchResult(): SearchResult {
        val payload = this.payloadMap
        return SearchResult(
            adlId = payload[PAYLOAD_ADL_ID]?.stringValue ?: "",
            example = payload[PAYLOAD_EXAMPLE]?.stringValue ?: "",
            score = this.score,
            content = payload[PAYLOAD_CONTENT]?.stringValue ?: "",
        )
    }

    companion object {
        private const val PAYLOAD_ADL_ID = "adl_id"
        private const val PAYLOAD_EXAMPLE = "example"
        private const val PAYLOAD_CONTENT = "content"
    }
}

