// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound.mutation

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import kotlinx.serialization.Serializable
import org.eclipse.lmos.adl.server.agents.TestVariant
import org.eclipse.lmos.adl.server.models.TestCase
import org.eclipse.lmos.adl.server.models.TestRunResult
import org.eclipse.lmos.adl.server.models.ConversationTurn
import org.eclipse.lmos.adl.server.repositories.AdlRepository
import org.eclipse.lmos.adl.server.services.TestExecutor
import org.eclipse.lmos.adl.server.repositories.TestCaseRepository
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.agent.process
import org.eclipse.lmos.arc.assistants.support.usecases.toUseCases
import org.eclipse.lmos.arc.core.getOrThrow
import org.slf4j.LoggerFactory

/**
 * GraphQL Mutation for creating test cases using the TestCreatorAgent.
 */
class TestCreatorMutation(
    private val testCreatorAgent: ConversationAgent,
    private val testCaseRepository: TestCaseRepository,
    private val testExecutor: TestExecutor,
    private val adlRepository: AdlRepository,
    private val testVariantAgent: ConversationAgent,
) : Mutation {

    private val log = LoggerFactory.getLogger(this::class.java)

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
        @GraphQLDescription("The ADL identifier") id: String,
    ): NewTestsResponse {
        val useCases = adlRepository.get(id)?.content?.toUseCases() ?: error("No adl found with id: $id")
        val testCases = useCases.flatMap { useCase ->
            testCreatorAgent.process<TestCreatorInput, List<TestCase>>(TestCreatorInput(useCase.toString()))
                .getOrThrow().map {
                    it.copy(useCaseId = useCase.id, adlId = id)
                }
        }.map { testCase ->
            val variants =
                testVariantAgent.process<String, TestVariant>(testCase.expectedConversation.filter { it.role == "user" }
                    .joinToString { "- ${it.content}" }).getOrThrow()
            log.info("Generated variants for test case ${testCase.id}: $variants")

            val newVariants = mutableListOf<List<ConversationTurn>>()
            repeat(8) { i ->
                newVariants += testCase.expectedConversation.map { turn ->
                    if (turn.role == "user") {
                        val variantContent = variants.variants[turn.content]?.getOrNull(i) ?: turn.content
                        turn.copy(content = variantContent)
                    } else {
                        turn
                    }
                }
            }
            testCase.copy(variants = newVariants)
        }

        testCaseRepository.saveAll(testCases)
        return NewTestsResponse(testCases.size)
    }

    @GraphQLDescription("Executes tests for a given Use Case.")
    suspend fun executeTests(
        @GraphQLDescription("The ADL identifier") adlId: String,
        @GraphQLDescription("The Test Case ID") testCaseId: String? = null,
    ): TestRunResult {
        return testExecutor.executeTests(adlId, testCaseId)
    }

    @GraphQLDescription("Deletes a test case by its ID.")
    suspend fun deleteTest(
        @GraphQLDescription("The ID of the test case to delete") id: String,
    ): Boolean {
        return testCaseRepository.delete(id)
    }

    @GraphQLDescription("Updates a single Test Case.")
    suspend fun updateTest(
        @GraphQLDescription("The updated Test Case data") input: UpdateTestCaseInput,
    ): TestCase {
        val existing = testCaseRepository.findById(input.id)
            ?: throw IllegalArgumentException("Test Case with ID ${input.id} not found")

        val updated = existing.copy(
            name = input.name ?: existing.name,
            description = input.description ?: existing.description,
            expectedConversation = input.expectedConversation ?: existing.expectedConversation,
            contract = input.contract ?: false
        )

        return testCaseRepository.save(updated)
    }

    @GraphQLDescription("Adds a new Test Case manually.")
    suspend fun addTest(
        @GraphQLDescription("The new Test Case data") input: AddTestCaseInput,
    ): TestCase {
        val testCase = TestCase(
            useCaseId = input.useCaseId,
            name = input.name,
            description = input.description,
            expectedConversation = input.expectedConversation,
            contract = input.contract ?: false,
        )
        return testCaseRepository.save(testCase)
    }
}

/**
 * Response for creating new tests.
 */
@Serializable
data class NewTestsResponse(
    @GraphQLDescription("The number of test cases created")
    val count: Int,
)

/**
 * Input for creating test cases.
 */
@Serializable
data class TestCreatorInput(
    val useCase: String,
)

/**
 * Input for updating test cases.
 */
@Serializable
data class UpdateTestCaseInput(
    @GraphQLDescription("The ID of the test case to update")
    val id: String,
    @GraphQLDescription("The new name of the test case")
    val name: String? = null,
    @GraphQLDescription("The new description of the test case")
    val description: String? = null,
    @GraphQLDescription("The new expected conversation")
    val expectedConversation: List<ConversationTurn>? = null,
    @GraphQLDescription("The contract flag")
    val contract: Boolean? = null,
)

/**
 * Input for adding test cases manually.
 */
@Serializable
data class AddTestCaseInput(
    @GraphQLDescription("The Use Case ID associated with this test")
    val useCaseId: String,
    @GraphQLDescription("The name of the test case")
    val name: String,
    @GraphQLDescription("The description of the test case")
    val description: String,
    @GraphQLDescription("The expected conversation")
    val expectedConversation: List<ConversationTurn>,
    @GraphQLDescription("The contract flag")
    val contract: Boolean? = null,
)
