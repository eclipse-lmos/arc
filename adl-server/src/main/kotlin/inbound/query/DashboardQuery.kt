// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.inbound.query

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import org.eclipse.lmos.adl.server.repositories.AdlRepository
import org.eclipse.lmos.adl.server.repositories.StatisticsRepository

class DashboardQuery(
    val adlRepository: AdlRepository,
    val statisticsRepository: StatisticsRepository
) : Query {

    @GraphQLDescription("Returns dashboard statistics")
    suspend fun dashboard(): DashboardStats {
        val totalAdls = adlRepository.list().size
        val mostUsed = statisticsRepository.getMostUsedUseCase()
        val averageResponseTime = statisticsRepository.getAverageResponseTime()

        return DashboardStats(
            numberOfAdls = totalAdls,
            mostUsedUseCase = mostUsed.map {
                 val scores = statisticsRepository.getComplianceScores(it.first)
                 UseCaseStats(it.first, it.second, scores?.first, scores?.second)
            },
            averageResponseTime = averageResponseTime
        )
    }
}

data class DashboardStats(
    val numberOfAdls: Int,
    val mostUsedUseCase: List<UseCaseStats>,
    val averageResponseTime: Double
)

data class UseCaseStats(
    val useCaseId: String,
    val count: Int,
    val minComplianceScore: Int? = null,
    val maxComplianceScore: Int? = null
)
