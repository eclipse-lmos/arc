// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.agent.process
import org.eclipse.lmos.arc.core.getOrThrow

/**
 * GraphQL Mutation for creating test cases using the TestCreatorAgent.
 */
class AdlTestCreatorMutation(
    private val testCreatorAgent: ConversationAgent,
) : Mutation {

    @GraphQLDescription("Generates test cases for a provided Use Case.")
    suspend fun createTests(
        @GraphQLDescription("The Use Case description") useCase: String,
    ): List<TestCase> {
        return testCreatorAgent.process<TestCreatorInput, List<TestCase>>(TestCreatorInput(useCase)).getOrThrow()
    }
}

@Serializable
data class TestCreatorInput(
    val useCase: String,
)

@Serializable
data class TestCase(
    val title: String,
    val description: String,
    @SerialName("expected_conversation")
    val expectedConversation: List<ConversationTurn>,
)

@Serializable
data class ConversationTurn(
    val role: String,
    val content: String,
)
