// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.repositories

import org.eclipse.lmos.adl.server.model.SimpleMessage

/**
 * Interface for storing UseCase embeddings.
 */
interface UseCaseEmbeddingsRepository : AutoCloseable {
    suspend fun initialize()
    suspend fun storeUtterances(id: String, examples: List<String>): Int
    suspend fun storeUseCase(adl: String, examples: List<String> = emptyList()): Int
    suspend fun search(query: String, limit: Int = 5, scoreThreshold: Float = 0.0f): List<UseCaseSearchResult>
    suspend fun searchByConversation(messages: List<SimpleMessage>, limit: Int = 5, scoreThreshold: Float = 0.0f): List<UseCaseSearchResult>
    suspend fun deleteByUseCaseId(useCaseId: String)
    suspend fun clear()
    suspend fun count(): Long
}

/**
 * Result of a UseCase similarity search.
 */
data class UseCaseSearchResult(
    val useCaseId: String,
    val example: String,
    val score: Float,
    val content: String,
)
