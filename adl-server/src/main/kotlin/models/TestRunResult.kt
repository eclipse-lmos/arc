// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.models

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import kotlinx.serialization.Serializable

/**
 * Result of a test suite execution.
 */
@Serializable
@GraphQLDescription("Result of a test suite execution")
data class TestRunResult(
    @GraphQLDescription("The overall score of the test suite (0-100)")
    val overallScore: Double,
    @GraphQLDescription("The results of individual test cases")
    val results: List<TestExecutionResult>,
)
