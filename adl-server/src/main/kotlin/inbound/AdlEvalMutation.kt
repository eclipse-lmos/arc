// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import kotlinx.serialization.Serializable
import org.eclipse.lmos.adl.server.agents.EvalOutput
import org.eclipse.lmos.arc.core.getOrThrow
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.agent.process

/**
 * GraphQL Query for creating test cases for a given use case.
 */
class AdlEvalMutation(
    private val testAgent: ConversationAgent
) : Mutation {

    @GraphQLDescription("Generates test cases for a given use case.")
    suspend fun eval(
        @GraphQLDescription("The use case description") input: EvalInput
    ): EvalOutput {
        return testAgent.process<EvalInput, EvalOutput>(input).getOrThrow()
    }
}

@Serializable
data class EvalInput(
    val useCase: String,
    val conversation: String,
)

