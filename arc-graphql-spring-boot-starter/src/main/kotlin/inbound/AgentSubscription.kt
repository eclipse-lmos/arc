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
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.User
import org.eclipse.lmos.arc.agents.agent.AgentHandoverLimit
import org.eclipse.lmos.arc.agents.agent.executeWithHandover
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.latest
import org.eclipse.lmos.arc.agents.dsl.extensions.OutputContext
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.events.MessagePublisherChannel
import org.eclipse.lmos.arc.agents.getAgentByName
import org.eclipse.lmos.arc.api.AgentRequest
import org.eclipse.lmos.arc.api.AgentResult
import org.eclipse.lmos.arc.api.AgentResultType.ERROR
import org.eclipse.lmos.arc.api.AgentResultType.EVENT
import org.eclipse.lmos.arc.api.AgentResultType.INTERMEDIATE_MESSAGE
import org.eclipse.lmos.arc.api.AgentResultType.MESSAGE
import org.eclipse.lmos.arc.api.ContextEntry
import org.eclipse.lmos.arc.api.EVENT_DATA_CONTEXT_KEY
import org.eclipse.lmos.arc.api.EVENT_TYPE_CONTEXT_KEY
import org.eclipse.lmos.arc.api.ToolCall
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.getOrThrow
import org.eclipse.lmos.arc.graphql.AgentResolver
import org.eclipse.lmos.arc.graphql.ContextHandler
import org.eclipse.lmos.arc.graphql.ErrorHandler
import org.eclipse.lmos.arc.graphql.combine
import org.eclipse.lmos.arc.graphql.context.AnonymizationEntities
import org.eclipse.lmos.arc.graphql.context.ContextProvider
import org.eclipse.lmos.arc.graphql.withLogContext
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * GraphQL Subscription for executing agents and streaming their results.
 * Handles agent selection, context injection, eventing, and intermediate message streaming.
 *
 * @property agentProvider Provider for available agents
 * @property errorHandler Optional error handler for agent execution errors
 * @property agentResolver Optional resolver for dynamic agent selection
 * @property agentHandoverRecursionLimit Maximum recursion depth for agent handover
 * @property eventPublisher Optional publisher for broadcasting events
 */
class AgentSubscription(
    private val agentProvider: AgentProvider,
    private val errorHandler: ErrorHandler? = null,
    contextHandlers: List<ContextHandler> = emptyList(),
    private val agentResolver: AgentResolver? = null,
    private val agentHandoverRecursionLimit: Int = 20,
    private val eventPublisher: EventPublisher? = null,
) : Subscription {

    private val log = LoggerFactory.getLogger(javaClass)
    private val combinedContextHandler = contextHandlers.combine()

    /**
     * Executes an agent and returns the results as a GraphQL subscription stream.
     * If no agent is specified, the first available agent is used.
     *
     * @param agentName Optional name of the agent to execute
     * @param request The agent request containing context and messages
     * @return A channel flow streaming [AgentResult]s
     */
    @GraphQLDescription("Executes an Agent and returns the results. If no agent is specified, the first agent is used.")
    fun agent(agentName: String? = null, request: AgentRequest) = channelFlow {
        coroutineScope {
            val agent = findAgent(agentName, request)
            val anonymizationEntities =
                AnonymizationEntities(request.conversationContext.anonymizationEntities.convertConversationEntities())
            val start = System.nanoTime()
            val messageChannel = Channel<AssistantMessage>()
            val outputContext = OutputContext()

            // Setup eventing that returns published events on the channel.
            val publisher = if (request.enableEventing) sendEvents(start) else null

            async {
                sendIntermediateMessage(messageChannel, start, anonymizationEntities)
            }

            val result = withLogContext(agent.name, request) {
                combinedContextHandler.inject(request) { extraContext ->
                    log.info("Received request: {request.systemContext}")
                    agent.executeWithHandover(
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
                            AgentHandoverLimit(agentHandoverRecursionLimit),
                            anonymizationEntities,
                            MessagePublisherChannel(messageChannel),
                            ContextProvider(request),
                            outputContext,
                        ).addIfNotNull(publisher) + extraContext + extractContext(request),
                        agentProvider,
                    )
                }
            }

            val responseTime = Duration.ofNanos(System.nanoTime() - start).toMillis() / 1000.0
            when (result) {
                is Success -> {
                    val outputMessage = result.value.latest<AssistantMessage>()
                    send(
                        AgentResult(
                            status = result.value.classification.toString(),
                            type = MESSAGE,
                            responseTime = responseTime,
                            messages = listOf(outputMessage.toMessage()),
                            anonymizationEntities = anonymizationEntities.entities.convertAPIEntities(),
                            context = outputContext.map().map { (key, value) -> ContextEntry(key, value) },
                            toolCalls = outputMessage?.toolCalls?.map { ToolCall(it.name, it.arguments) },
                        ),
                    )
                }

                is Failure -> {
                    val handledResult = (errorHandler?.handleError(result.reason) ?: result).getOrThrow()
                    send(
                        AgentResult(
                            type = ERROR,
                            responseTime = responseTime,
                            messages = listOf(handledResult.toMessage()),
                            anonymizationEntities = emptyList(),
                            context = outputContext.map().map { (key, value) -> ContextEntry(key, value) },
                        ),
                    )
                }
            }
            messageChannel.close()
        }
    }

    /**
     * Adds an item to the set if it is not null.
     *
     * @receiver The original set
     * @param item The item to add if not null
     * @return The set with the item added if not null
     */
    private fun Set<Any>.addIfNotNull(item: Any?): Set<Any> {
        return if (item != null) this + item else this
    }

    /**
     * Finds the appropriate [ConversationAgent] based on the agent name or request.
     * Falls back to the first available agent if none is found.
     *
     * @param agentName Optional agent name
     * @param request The agent request
     * @return The resolved [ConversationAgent]
     * @throws IllegalStateException if no agent is defined
     */
    private fun findAgent(agentName: String?, request: AgentRequest): ConversationAgent =
        agentName?.let { agentProvider.getAgentByName(it) } as ConversationAgent?
            ?: agentResolver?.resolveAgent(agentName, request) as ConversationAgent?
            ?: agentProvider.getAgents().firstOrNull() as ConversationAgent?
            ?: error("No Agent defined!")

    /**
     * Sends intermediate [AssistantMessage]s as [AgentResult]s to the client channel.
     *
     * @param messageChannel Channel of [AssistantMessage]s
     * @param startTime Start time for response time calculation
     * @param anonymizationEntities Entities for anonymization in the result
     */
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
                    type = INTERMEDIATE_MESSAGE,
                    responseTime = responseTime,
                    messages = listOf(message.toMessage()),
                    anonymizationEntities = anonymizationEntities.entities.convertAPIEntities(),
                ),
            )
        }
    }

    /**
     * Creates an [EventPublisher] that sends events as [AgentResult]s to the client channel.
     *
     * @param startTime Start time for response time calculation
     * @return [EventPublisher] that streams events as [AgentResult]s
     */
    private fun ProducerScope<AgentResult>.sendEvents(startTime: Long): EventPublisher {
        return EventPublisher { event ->
            try {
                val responseTime = Duration.ofNanos(System.nanoTime() - startTime).toMillis() / 1000.0
                log.debug("Sending event to client: $event")
                trySend(
                    AgentResult(
                        type = EVENT,
                        responseTime = responseTime,
                        messages = emptyList(),
                        context = listOf(
                            ContextEntry(EVENT_TYPE_CONTEXT_KEY, event::class.java.simpleName),
                            ContextEntry(EVENT_DATA_CONTEXT_KEY, event.toJson()),
                        ),
                    ),
                )
            } catch (e: Exception) {
                log.error("Error while sending event to request channel!", e)
            }
            eventPublisher?.publish(event)
        }
    }
}

/**
 * Extension property to check if eventing is enabled in the AgentRequest's system context.
 *
 * @receiver [AgentRequest]
 * @return true if eventing is enabled, false otherwise
 */
val AgentRequest.enableEventing: Boolean
    get() = systemContext.any { it.key == "enableEventing" && it.value.equals("true", ignoreCase = true) }
