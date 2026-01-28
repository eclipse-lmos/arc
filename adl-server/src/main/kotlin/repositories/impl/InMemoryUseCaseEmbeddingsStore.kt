// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.repositories.impl

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.store.embedding.EmbeddingSearchRequest
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import org.eclipse.lmos.adl.server.models.SimpleMessage
import org.eclipse.lmos.adl.server.repositories.UseCaseEmbeddingsRepository
import org.eclipse.lmos.adl.server.repositories.UseCaseSearchResult
import org.eclipse.lmos.arc.assistants.support.usecases.toUseCases
import java.util.concurrent.ConcurrentHashMap

class InMemoryUseCaseEmbeddingsStore(
    private val embeddingModel: EmbeddingModel
) : UseCaseEmbeddingsRepository {

    private val store = InMemoryEmbeddingStore<TextSegment>()
    // Manage mapping of UseCaseId to embedding IDs in the store
    private val useCaseIdToEmbeddingIds = ConcurrentHashMap<String, MutableList<String>>()

    override suspend fun initialize() {
        // In-Memory does not require initialization
    }

    override suspend fun storeUtterances(id: String, examples: List<String>): Int {
        deleteByUseCaseId(id)
        return storeExamples(id, "", examples)
    }

    override suspend fun storeUseCase(adl: String, examples: List<String>): Int {
        val parsedUseCases = adl.toUseCases()
        var count = 0
        parsedUseCases.forEach { useCase ->
            deleteByUseCaseId(useCase.id)
            val allExamples = (parseExamples(useCase.examples) + examples).distinct()
            count += storeExamples(useCase.id, adl, allExamples)
        }
        return count
    }

    private fun storeExamples(useCaseId: String, content: String, examples: List<String>): Int {
        val segments = examples.map { example ->
            TextSegment.from(example, Metadata.from(mapOf(
                PAYLOAD_USECASE_ID to useCaseId,
                PAYLOAD_EXAMPLE to example,
                PAYLOAD_CONTENT to content
            )))
        }

        if (segments.isEmpty()) return 0

        val embeddings = embeddingModel.embedAll(segments).content()
        val ids = store.addAll(embeddings, segments)

        useCaseIdToEmbeddingIds.computeIfAbsent(useCaseId) { mutableListOf() }.addAll(ids)

        return ids.size
    }

    override suspend fun search(query: String, limit: Int, scoreThreshold: Float): List<UseCaseSearchResult> {
        val embedding = embeddingModel.embed(query).content()
        val request = EmbeddingSearchRequest.builder()
            .queryEmbedding(embedding)
            .maxResults(limit)
            .minScore(scoreThreshold.toDouble())
            .build()
        val results = store.search(request).matches()

        return results.map { match ->
            UseCaseSearchResult(
                useCaseId = match.embedded().metadata().getString(PAYLOAD_USECASE_ID) ?: "",
                example = match.embedded().metadata().getString(PAYLOAD_EXAMPLE) ?: "",
                score = match.score().toFloat(),
                content = match.embedded().metadata().getString(PAYLOAD_CONTENT) ?: ""
            )
        }
    }

    override suspend fun searchByConversation(
        messages: List<SimpleMessage>,
        limit: Int,
        scoreThreshold: Float
    ): List<UseCaseSearchResult> {
         // Filter last user messages
         return messages.filter { it.role == "user" && it.content.length > 5 }
            .takeLast(5)
            .flatMap { search(it.content, limit, scoreThreshold) }
    }

    override suspend fun deleteByUseCaseId(useCaseId: String) {
        val ids = useCaseIdToEmbeddingIds.remove(useCaseId)
        if (!ids.isNullOrEmpty()) {
            store.removeAll(ids)
        }
    }

    override suspend fun clear() {
        val allIds = useCaseIdToEmbeddingIds.values.flatten()
        if (allIds.isNotEmpty()) {
            store.removeAll(allIds)
            useCaseIdToEmbeddingIds.clear()
        }
    }

    override suspend fun count(): Long {
        return useCaseIdToEmbeddingIds.values.sumOf { it.size }.toLong()
    }

    override fun close() {
        // Nothing to do for In-Memory
    }

    private fun parseExamples(examples: String): List<String> {
        return examples.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.removePrefix("-").trim() }
            .filter { it.isNotEmpty() }
    }

    companion object {
        private const val PAYLOAD_USECASE_ID = "usecase_id"
        private const val PAYLOAD_EXAMPLE = "example"
        private const val PAYLOAD_CONTENT = "content"
    }
}
