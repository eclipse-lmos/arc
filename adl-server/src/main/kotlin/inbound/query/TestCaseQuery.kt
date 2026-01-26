// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound.query

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import org.eclipse.lmos.adl.server.models.TestCase
import org.eclipse.lmos.adl.server.model.SimpleMessage
import org.eclipse.lmos.adl.server.repositories.TestCaseRepository

@GraphQLDescription("GraphQL Query for fetching test cases for a use case.")
class TestCaseQuery(
    private val testCaseRepository: TestCaseRepository,
)  : Query {

    @GraphQLDescription("Fetches test cases for a given use case ID.")
   suspend fun testCases(
        @GraphQLDescription("The ID of the use case.") useCaseId: String
    ): List<TestCase> {
        return testCaseRepository.findByUseCaseId(useCaseId)
    }
}
