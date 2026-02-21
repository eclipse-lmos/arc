// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound.mutation

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import kotlinx.serialization.Serializable
import org.eclipse.lmos.adl.server.repositories.AdlRepository
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.User
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.conversation.latest
import org.eclipse.lmos.arc.agents.dsl.extensions.OutputContext
import org.eclipse.lmos.arc.agents.events.LoggingEventHandler
import org.eclipse.lmos.arc.api.AgentRequest
import org.eclipse.lmos.arc.api.AgentResult
import org.eclipse.lmos.arc.api.AgentResultType.MESSAGE
import org.eclipse.lmos.arc.api.ContextEntry
import org.eclipse.lmos.arc.api.Message
import org.eclipse.lmos.arc.api.ToolCall
import org.eclipse.lmos.arc.assistants.support.usecases.toUseCases
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Success
import java.time.Duration

class AdlAssistantMutation(
    private val assistantAgent: ConversationAgent,
    private val adlStorage: AdlRepository,
) : Mutation {

    private val log = org.slf4j.LoggerFactory.getLogger(this.javaClass)

    @GraphQLDescription("Calls the assistant agent")
    suspend fun assistant(
        @GraphQLDescription("The assistant input") input: AssistantInput,
    ): AgentResult {
        log.info("Received assistant request with useCases: ${input.request.conversationContext.conversationId}")
        val useCases =
            input.useCasesId?.let { adlStorage.getAsUseCases(it) } ?: input.useCases?.toUseCases() ?: emptyList()
        val request = input.request
        val outputContext = OutputContext()
        val start = System.nanoTime()

        val result = assistantAgent.execute(
            Conversation(
                user = request.userContext.userId?.let { User(it) },
                conversationId = request.conversationContext.conversationId,
                currentTurnId = request.conversationContext.turnId
                    ?: request.messages.lastOrNull()?.turnId
                    ?: request.messages.size.toString(),
                transcript = request.messages.convert(),
            ),
            setOf(
                request,
                useCases,
                outputContext,
            ),
        )

        val responseTime = Duration.ofNanos(System.nanoTime() - start).toMillis() / 1000.0
        return when (result) {
            is Success -> {
                val outputMessage = result.value.latest<AssistantMessage>()
                AgentResult(
                    status = result.value.classification.toString(),
                    type = MESSAGE,
                    responseTime = responseTime,
                    messages = listOf(outputMessage.toMessage()),
                    context = outputContext.map().map { (key, value) -> ContextEntry(key, value) },
                    toolCalls = outputMessage?.toolCalls?.map { ToolCall(it.name, it.arguments) },
                )
            }

            is Failure -> throw result.reason
        }
    }
}

@Serializable
data class AssistantInput(val useCases: String? = null, val useCasesId: String? = null, val request: AgentRequest)

fun List<Message>.convert(): List<ConversationMessage> = map {
    when (it.role) {
        "user" -> UserMessage(it.content)
        "assistant" -> AssistantMessage(it.content)
        else -> throw IllegalArgumentException("Unknown role: ${it.role}")
    }
}

fun AssistantMessage?.toMessage() = Message("assistant", this?.content ?: "", turnId = this?.turnId)
