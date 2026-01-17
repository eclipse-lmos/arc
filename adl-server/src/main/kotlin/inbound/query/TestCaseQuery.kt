// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound.query

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import org.eclipse.lmos.adl.server.inbound.SimpleMessage
import org.eclipse.lmos.adl.server.inbound.mutation.TestCase

@GraphQLDescription("GraphQL Query for fetching test cases for a use case.")
class TestCaseQuery : Query {

    @GraphQLDescription("Fetches test cases for a given use case ID.")
    fun testCases(
        @GraphQLDescription("The ID of the use case.") useCaseId: String
    ): List<TestCase> {
        // Mock data for now
        return listOf(
            TestCase(
                id = "1",
                name = "Basic Greeting",
                description = "A simple conversation to test the AI's greeting capabilities.",
                expectedConversation = listOf(
                    SimpleMessage(role = "user", content = "Hello!"),
                    SimpleMessage(role = "assistant", content = "Hi there! How can I help you today?")
                )
            )
        )
    }
}