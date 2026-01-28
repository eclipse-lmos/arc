// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import org.eclipse.lmos.adl.server.models.TestCase
import org.eclipse.lmos.adl.server.repositories.TestCaseRepository

/**
 * GraphQL Query for retrieving Test Cases.
 */
class AdlTestQuery(
    private val testCaseRepository: TestCaseRepository,
) : Query {

    @GraphQLDescription("Retrieves test cases associated with a specific Use Case ID.")
    suspend fun getTests(
        @GraphQLDescription("The ID of the Use Case") useCaseId: String,
    ): List<TestCase> {
        return testCaseRepository.findByUseCaseId(useCaseId)
    }
}
