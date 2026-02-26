// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.repositories.impl

import org.eclipse.lmos.adl.server.repositories.StatisticsRepository
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class InMemoryStatisticsRepository : StatisticsRepository {

    private val useCaseCounts = ConcurrentHashMap<String, AtomicLong>()
    private val useCaseCompilanceScores = ConcurrentHashMap<String, MutableList<Int>>()
    private val totalResponseTimeMillis = AtomicLong(0)
    private val requestCount = AtomicLong(0)

    override suspend fun incrementUseCaseCount(useCaseId: String) {
        useCaseCounts.computeIfAbsent(useCaseId) { AtomicLong(0) }.incrementAndGet()
    }

    override suspend fun recordResponseTime(duration: Duration) {
        totalResponseTimeMillis.addAndGet(duration.toMillis())
        requestCount.incrementAndGet()
    }

    override suspend fun recordComplianceScore(useCaseId: String, score: Int) {
        useCaseCompilanceScores.computeIfAbsent(useCaseId) { java.util.Collections.synchronizedList(ArrayList()) }
            .add(score)
    }

    override suspend fun getMostUsedUseCase(): List<Pair<String, Int>> {
        return useCaseCounts.entries
            .map { (key, value) -> key to value.get().toInt() }
            .sortedByDescending { it.second }
    }

    override suspend fun getAverageResponseTime(): Double {
        val count = requestCount.get()
        return if (count > 0) {
            totalResponseTimeMillis.get().toDouble() / count
        } else {
            0.0
        }
    }

    override suspend fun getComplianceScores(useCaseId: String): Pair<Int, Int>? {
        val scores = useCaseCompilanceScores[useCaseId] ?: return null
        synchronized(scores) {
            if (scores.isEmpty()) return null
            return scores.min() to scores.max()
        }
    }
}
