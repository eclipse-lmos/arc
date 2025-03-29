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
import org.eclipse.lmos.arc.agents.AgentFailedException
import org.eclipse.lmos.arc.agents.AgentProvider
import org.eclipse.lmos.arc.agents.ChatAgent
import org.eclipse.lmos.arc.agents.User
import org.eclipse.lmos.arc.agents.conversation.AIAgentHandover
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.latest
import org.eclipse.lmos.arc.agents.dsl.extensions.OutputContext
import org.eclipse.lmos.arc.agents.events.MessagePublisherChannel
import org.eclipse.lmos.arc.agents.getAgentByName
import org.eclipse.lmos.arc.api.AgentRequest
import org.eclipse.lmos.arc.api.AgentResult
import org.eclipse.lmos.arc.api.ContextEntry
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.getOrNull
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

class AgentSubscription(
    private val agentProvider: AgentProvider,
    private val errorHandler: ErrorHandler? = null,
    contextHandlers: List<ContextHandler> = emptyList(),
    private val agentResolver: AgentResolver? = null,
    private val agentHandoverRecursionLimit: Int = 20,
) : Subscription {

    private val log = LoggerFactory.getLogger(javaClass)
    private val combinedContextHandler = contextHandlers.combine()

    @GraphQLDescription("Executes an Agent and returns the results. If no agent is specified, the first agent is used.")
    fun agent(agentName: String? = null, request: AgentRequest) = channelFlow {
        coroutineScope {
            val agent = findAgent(agentName, request)
            val anonymizationEntities =
                AnonymizationEntities(request.conversationContext.anonymizationEntities.convertConversationEntities())
            val start = System.nanoTime()
            val messageChannel = Channel<AssistantMessage>()
            val outputContext = OutputContext()

            log.info("Received request: ${request.systemContext}")

            async {
                sendIntermediateMessage(messageChannel, start, anonymizationEntities)
            }

            val result = combinedContextHandler.inject(request) { extraContext ->
                withLogContext(agent.name, request) {
                    val output = agent.execute(
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
                            outputContext,
                        ) + extraContext,
                    )
                    handleAIAgentHandover(output, extraContext)
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
                        context = outputContext.map().map { (key, value) -> ContextEntry(key, value) },
                    ),
                )

                is Failure -> {
                    val handledResult = (errorHandler?.handleError(result.reason) ?: result).getOrThrow()
                    send(
                        AgentResult(
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

    private suspend fun handleAIAgentHandover(
        output: Result<Conversation, AgentFailedException>,
        context: Set<Any>,
        recursionCount: Int = 0,
    ): Result<Conversation, AgentFailedException> {
        if (recursionCount > agentHandoverRecursionLimit) {
            log.error("Recursion limit reached for agent handover! Stopping here and returning current result.")
            return output
        }
        output.getOrNull()?.takeIf { it.classification is AIAgentHandover }?.let { conversation ->
            val handover = conversation.classification as AIAgentHandover
            log.info("Agent handover to $handover")
            val nextAgent = agentProvider.getAgentByName(handover.name) as? ChatAgent?
            if (nextAgent != null) {
                val newOutput = nextAgent.execute(conversation.copy(classification = null), context)
                return handleAIAgentHandover(newOutput, context, recursionCount + 1)
            }
        }
        return output
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
