// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.graphql.inbound

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Subscription
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.channelFlow
import org.eclipse.lmos.arc.agents.AgentProvider
import org.eclipse.lmos.arc.agents.ChatAgent
import org.eclipse.lmos.arc.agents.User
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.latest
import org.eclipse.lmos.arc.agents.events.MessagePublisherChannel
import org.eclipse.lmos.arc.agents.getAgentByName
import org.eclipse.lmos.arc.api.AgentRequest
import org.eclipse.lmos.arc.api.AgentResult
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.getOrThrow
import org.eclipse.lmos.arc.graphql.AgentResolver
import org.eclipse.lmos.arc.graphql.ContextHandler
import org.eclipse.lmos.arc.graphql.EmptyContextHandler
import org.eclipse.lmos.arc.graphql.ErrorHandler
import org.eclipse.lmos.arc.graphql.context.AnonymizationEntities
import org.eclipse.lmos.arc.graphql.context.ContextProvider
import org.eclipse.lmos.arc.graphql.withLogContext
import org.slf4j.LoggerFactory
import java.time.Duration

class AgentSubscription(
    private val agentProvider: AgentProvider,
    private val errorHandler: ErrorHandler? = null,
    private val contextHandler: ContextHandler = EmptyContextHandler(),
    private val agentResolver: AgentResolver? = null,
) : Subscription {

    private val log = LoggerFactory.getLogger(javaClass)

    @GraphQLDescription("Executes an Agent and returns the results. If no agent is specified, the first agent is used.")
    fun agent(agentName: String? = null, request: AgentRequest) = channelFlow {
        coroutineScope {
            val agent = findAgent(agentName, request)
            val anonymizationEntities =
                AnonymizationEntities(request.conversationContext.anonymizationEntities.convertConversationEntities())
            val start = System.nanoTime()
            val messageChannel = Channel<AssistantMessage>()

            log.info("Received request: ${request.systemContext}")

            async {
                sendIntermediateMessage(messageChannel, start, anonymizationEntities)
            }

            val result = contextHandler.inject(request) {
                withLogContext(agent.name, request) {
                    agent.execute(
                        Conversation(
                            user = request.userContext.userId?.let { User(it) },
                            conversationId = request.conversationContext.conversationId,
                            currentTurnId = request.conversationContext.turnId
                                ?: request.messages.lastOrNull()?.turnId
                                ?: request.messages.size.toString(),
                            transcript = request.messages.convert(),
                            anonymizationEntities = anonymizationEntities.entities,
                        ),
                        setOf(
                            request,
                            anonymizationEntities,
                            MessagePublisherChannel(messageChannel),
                            ContextProvider(request),
                        ),
                    )
                }
            }

            val responseTime = Duration.ofNanos(System.nanoTime() - start).toMillis() / 1000.0
            when (result) {
                is Success -> send(
                    AgentResult(
                        status = result.value.classification.toString(),
                        responseTime = responseTime,
                        messages = listOf(result.value.latest<AssistantMessage>().toMessage()),
                        anonymizationEntities = anonymizationEntities.entities.convertAPIEntities(),
                    ),
                )

                is Failure -> {
                    val handledResult = (errorHandler?.handleError(result.reason) ?: result).getOrThrow()
                    send(
                        AgentResult(
                            responseTime = responseTime,
                            messages = listOf(handledResult.toMessage()),
                            anonymizationEntities = emptyList(),
                        ),
                    )
                }
            }
            messageChannel.close()
        }
    }

    private fun findAgent(agentName: String?, request: AgentRequest): ChatAgent =
        agentName?.let { agentProvider.getAgentByName(it) } as ChatAgent?
            ?: agentResolver?.resolveAgent(agentName, request) as ChatAgent?
            ?: agentProvider.getAgents().firstOrNull() as ChatAgent?
            ?: error("No Agent defined!")

    private suspend fun ProducerScope<AgentResult>.sendIntermediateMessage(
        messageChannel: Channel<AssistantMessage>,
        startTime: Long,
        anonymizationEntities: AnonymizationEntities,
    ) {
        for (message in messageChannel) {
            log.debug("Sending intermediate message: $message")
            val responseTime = Duration.ofNanos(System.nanoTime() - startTime).toMillis() / 1000.0
            trySend(
                AgentResult(
                    responseTime = responseTime,
                    messages = listOf(message.toMessage()),
                    anonymizationEntities = anonymizationEntities.entities.convertAPIEntities(),
                ),
            )
        }
    }
}
