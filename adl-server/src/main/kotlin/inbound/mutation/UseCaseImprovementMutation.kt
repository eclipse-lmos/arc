// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.inbound.mutation

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.agent.process
import org.eclipse.lmos.arc.core.getOrThrow

/**
 * GraphQL mutation for improving use cases.
 */
class UseCaseImprovementMutation(private val improvementAgent: ConversationAgent) : Mutation {

    /**
     * Analyzes the provided Use Case and suggests improvements.
     * @param useCase The Use Case content to analyze.
     * @return A UseCaseImprovementResponse object containing the suggested improvements.
     */
    @GraphQLDescription("Analyzes the provided Use Case and suggests improvements.")
    suspend fun improveUseCase(useCase: String): UseCaseImprovementResponse {
        return improvementAgent.process<String, UseCaseImprovementResponse>(useCase).getOrThrow()
    }
}

@Serializable
@GraphQLDescription("Response containing improvements for a Use Case")
data class UseCaseImprovementResponse(
    val improvements: List<UseCaseImprovement>
)

@Serializable
@GraphQLDescription("A specific improvement suggestion for a Use Case")
data class UseCaseImprovement(
    val issue: String,
    val suggestion: String,
    val improved_use_case: String
)
