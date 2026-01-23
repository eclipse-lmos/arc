// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import kotlinx.serialization.Serializable
import org.eclipse.lmos.adl.server.agents.EvalOutput
import org.eclipse.lmos.adl.server.services.ConversationEvaluator
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.agent.process
import org.eclipse.lmos.arc.core.getOrThrow

/**
 * GraphQL Query for evaluating conversations.
 */
class AdlEvalMutation(
    private val testAgent: ConversationAgent,
    private val conversationEvaluator: ConversationEvaluator,
) : Mutation {

    @GraphQLDescription("Evaluates a conversation against a use case.")
    suspend fun eval(
        @GraphQLDescription("The use case") input: EvalInput,
    ): EvalOutput {
        return testAgent.process<EvalInput, EvalOutput>(input).getOrThrow()
    }

    @GraphQLDescription("Evaluates a conversation against an expected conversation.")
    suspend fun evalConversation(
        @GraphQLDescription("The actual conversation to be evaluated. Should contain a list of messages with role (e.g. 'user', 'assistant') and content.") conversation: List<SimpleMessage>,
        @GraphQLDescription("The expected conversation to compare against as a ground truth. Must have matching roles and semantically similar content for a high score.") expectedConversation: List<SimpleMessage>,
        @GraphQLDescription("The similarity threshold for failure. Defaults to 0.8.") failureThreshold: Double? = 0.8,
        @GraphQLDescription("If true, filters out all messages with role 'user' before evaluation. Defaults to false.") filterUserMessage: Boolean? = false,
    ): EvalOutput {
        val filteredConversation = if (filterUserMessage == true) conversation.filter { it.role != "user" } else conversation
        val filteredExpectedConversation = if (filterUserMessage == true) expectedConversation.filter { it.role != "user" } else expectedConversation

        return conversationEvaluator.evaluate(
            filteredConversation,
            filteredExpectedConversation,
            failureThreshold = failureThreshold ?: 0.8,
        )
    }

}

@Serializable
data class EvalInput(
    val useCase: String,
    val conversation: String,
)
