// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package ai.ancf.lmos.arc.graphql.inbound

import ai.ancf.lmos.arc.agents.AgentProvider
import ai.ancf.lmos.arc.agents.ChatAgent
import ai.ancf.lmos.arc.agents.User
import ai.ancf.lmos.arc.agents.conversation.AssistantMessage
import ai.ancf.lmos.arc.agents.conversation.Conversation
import ai.ancf.lmos.arc.agents.conversation.latest
import ai.ancf.lmos.arc.agents.getAgentByName
import ai.ancf.lmos.arc.api.AgentRequest
import ai.ancf.lmos.arc.api.AgentResult
import ai.ancf.lmos.arc.core.Failure
import ai.ancf.lmos.arc.core.Success
import ai.ancf.lmos.arc.core.getOrThrow
import ai.ancf.lmos.arc.graphql.ContextHandler
import ai.ancf.lmos.arc.graphql.EmptyContextHandler
import ai.ancf.lmos.arc.graphql.ErrorHandler
import ai.ancf.lmos.arc.graphql.context.AnonymizationEntities
import ai.ancf.lmos.arc.graphql.withLogContext
import com.expediagroup.graphql.server.operations.Subscription
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory

class AgentSubscription(
    private val agentProvider: AgentProvider,
    private val errorHandler: ErrorHandler? = null,
    private val contextHandler: ContextHandler = EmptyContextHandler(),
) : Subscription {

    private val log = LoggerFactory.getLogger(javaClass)

    fun agent(agentName: String? = null, request: AgentRequest) = flow {
        val agent = findAgent(agentName)
        val anonymizationEntities =
            AnonymizationEntities(request.conversationContext.anonymizationEntities.convertConversationEntities())

        log.info("Received request: ${request.systemContext}")

        val result = contextHandler.inject(request) {
            withLogContext(request) {
                agent.execute(
                    Conversation(
                        user = User(request.userContext.userId),
                        transcript = request.messages.convert(),
                        anonymizationEntities = anonymizationEntities.entities,
                    ),
                    setOf(request, anonymizationEntities),
                )
            }
        }
        when (result) {
            is Success -> emit(
                AgentResult(
                    messages = listOf(result.value.latest<AssistantMessage>().toMessage()),
                    anonymizationEntities = anonymizationEntities.entities.convertAPIEntities(),
                ),
            )

            is Failure -> {
                val handledResult = (errorHandler?.handleError(result.reason) ?: result).getOrThrow()
                emit(
                    AgentResult(
                        messages = listOf(handledResult.toMessage()),
                        anonymizationEntities = emptyList(),
                    ),
                )
            }
        }
    }

    private fun findAgent(agentName: String?): ChatAgent =
        agentName?.let { agentProvider.getAgentByName(it) } as ChatAgent?
            ?: agentProvider.getAgents().firstOrNull() as ChatAgent?
            ?: error("No Agent defined!")
}
