// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.search.embeddings

/**
 * Interface for storing UseCase embeddings.
 */
interface UseCaseEmbeddingsStore {
    suspend fun storeUseCase(adl: String): Int
    suspend fun deleteByUseCaseId(useCaseId: String)
    suspend fun clear()
}
