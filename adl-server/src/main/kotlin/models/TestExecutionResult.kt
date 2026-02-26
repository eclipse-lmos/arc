// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.models

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import kotlinx.serialization.Serializable
import org.eclipse.lmos.adl.server.agents.EvalOutput

/**
 * Result of a test execution.
 */
@Serializable
@GraphQLDescription("Result of a test execution")
data class TestExecutionResult(
    @GraphQLDescription("The ID of the test case")
    val testCaseId: String,
    @GraphQLDescription("The Name of the test case")
    val testCaseName: String,
    @GraphQLDescription("The status of the test execution (PASS/FAIL)")
    val status: String,
    @GraphQLDescription("The evaluation score")
    val score: Int,
    @GraphQLDescription("The actual conversation that took place")
    val actualConversation: List<ConversationTurn>,
    @GraphQLDescription("The use cases involved in the test execution")
    val useCases: List<String>,
    @GraphQLDescription("Detailed evaluation output")
    val details: EvalOutput,
)
