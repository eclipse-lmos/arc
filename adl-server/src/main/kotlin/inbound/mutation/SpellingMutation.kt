// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound.mutation

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.agent.ask
import org.eclipse.lmos.arc.agents.agent.process
import org.eclipse.lmos.arc.core.getOrThrow

/**
 * GraphQL Mutation for correcting spelling using the SpellingAgent.
 */
class SpellingMutation(
    private val spellingAgent: ConversationAgent,
) : Mutation {

    @GraphQLDescription("Corrects spelling and grammar of the provided text.")
    suspend fun correctSpelling(
        @GraphQLDescription("The text to correct") text: String,
    ): String {
        return spellingAgent.ask(text).getOrThrow()
    }
}
