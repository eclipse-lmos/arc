// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.repositories

import java.time.Duration

interface StatisticsRepository {
    suspend fun incrementUseCaseCount(useCaseId: String)
    suspend fun recordResponseTime(duration: Duration)
    suspend fun recordComplianceScore(useCaseId: String, score: Int)
    suspend fun getMostUsedUseCase(): List<Pair<String, Int>>
    suspend fun getAverageResponseTime(): Double
    suspend fun getComplianceScores(useCaseId: String): Pair<Int, Int>?
}
