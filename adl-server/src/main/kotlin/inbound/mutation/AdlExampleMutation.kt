// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound.mutation

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.core.getOrThrow

/**
 * GraphQL Query for creating examples for ADL UseCases.
 */
class AdlExampleMutation(
    private val exampleAgent: ConversationAgent,
) : Mutation {

    @GraphQLDescription("Generates examples for a given use case.")
    suspend fun examples(
        @GraphQLDescription("The the use case.") useCase: String,
    ): UseCaseExample {
        val conversation = Conversation() + UserMessage(useCase)
        val content = exampleAgent.execute(conversation)
            .getOrThrow()
            .transcript
            .filterIsInstance<AssistantMessage>()
            .lastOrNull()
            ?.content ?: ""

        return UseCaseExample(
            useCase = useCase,
            examples = content.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() && it.startsWith("- ") }
                .map { it.substringAfter("-") },
        )
    }
}

data class UseCaseExample(
    val useCase: String,
    val examples: List<String>,
)
