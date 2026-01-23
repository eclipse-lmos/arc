// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound.mutation

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.agent.process
import org.eclipse.lmos.arc.assistants.support.usecases.toUseCases
import org.eclipse.lmos.arc.core.getOrThrow
import org.eclipse.lmos.adl.server.repositories.TestCaseRepository

/**
 * GraphQL Mutation for creating test cases using the TestCreatorAgent.
 */
class AdlTestCreatorMutation(
    private val testCreatorAgent: ConversationAgent,
    private val testCaseRepository: TestCaseRepository,
) : Mutation {

    @GraphQLDescription("Generates test cases for a provided Use Case.")
    suspend fun createTests(
        @GraphQLDescription("The Use Case description") useCase: String,
    ): List<TestCase> {
        val useCaseId = useCase.toUseCases().firstOrNull()?.id ?: "unknown"
        return testCreatorAgent.process<TestCreatorInput, List<TestCase>>(TestCreatorInput(useCase)).getOrThrow().map {
            it.copy(useCaseId = useCaseId)
        }
    }

    @GraphQLDescription("Generates test cases for a provided Use Case and stores them in the repository.")
    suspend fun newTests(
        @GraphQLDescription("The Use Case description.") useCase: String,
    ): NewTestsResponse {
        val useCaseId = useCase.toUseCases().firstOrNull()?.id ?: "unknown"
        val testCases =
            testCreatorAgent.process<TestCreatorInput, List<TestCase>>(TestCreatorInput(useCase)).getOrThrow().map {
                it.copy(useCaseId = useCaseId)
            }
        testCases.forEach { testCaseRepository.save(it) }
        return NewTestsResponse(testCases.size, useCaseId)
    }
}

/**
 * Response for creating new tests.
 */
@Serializable
data class NewTestsResponse(
    @GraphQLDescription("The number of test cases created")
    val count: Int,
    @GraphQLDescription("The ID of the use case associated with these tests")
    val useCaseId: String,
)

/**
 * Input for creating test cases.
 */
@Serializable
data class TestCreatorInput(
    val useCase: String,
)

/**
 * Represents a generated test case.
 */
@Serializable
data class TestCase(
    @GraphQLDescription("The unique identifier of the test case")
    val id: String = java.util.UUID.randomUUID().toString(),
    @GraphQLDescription("The ID of the use case this test belongs to")
    val useCaseId: String? = null,
    @GraphQLDescription("The title of the test case")
    val name: String,
    @GraphQLDescription("The description of the test case")
    val description: String,
    @SerialName("expected_conversation")
    @GraphQLDescription("The expected conversation flow")
    val expectedConversation: List<ConversationTurn>,
)

/**
 * Represents a turn in a conversation.
 */
@Serializable
data class ConversationTurn(
    @GraphQLDescription("The role of the speaker (e.g., user, assistant)")
    val role: String,
    @GraphQLDescription("The content of the message")
    val content: String,
)
