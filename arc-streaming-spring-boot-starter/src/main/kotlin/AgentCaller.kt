// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.ws

import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.channelFlow
import org.eclipse.lmos.arc.agents.AgentProvider
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.User
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.DataStream
import org.eclipse.lmos.arc.agents.conversation.latest
import org.eclipse.lmos.arc.agents.events.MessagePublisherChannel
import org.eclipse.lmos.arc.agents.getAgentByName
import org.eclipse.lmos.arc.api.AgentRequest
import org.eclipse.lmos.arc.api.AgentResult
import org.eclipse.lmos.arc.api.Message
import org.eclipse.lmos.arc.api.userMessage
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.getOrThrow
import org.eclipse.lmos.arc.graphql.AgentResolver
import org.eclipse.lmos.arc.graphql.ContextHandler
import org.eclipse.lmos.arc.graphql.EmptyContextHandler
import org.eclipse.lmos.arc.graphql.ErrorHandler
import org.eclipse.lmos.arc.graphql.context.AnonymizationEntities
import org.eclipse.lmos.arc.graphql.context.ContextProvider
import org.eclipse.lmos.arc.graphql.inbound.convert
import org.eclipse.lmos.arc.graphql.inbound.convertAPIEntities
import org.eclipse.lmos.arc.graphql.inbound.convertConversationEntities
import org.eclipse.lmos.arc.graphql.inbound.toMessage
import org.eclipse.lmos.arc.graphql.withLogContext
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * Calls the appropriate agent and converts the output to an AgentResult.
 */
class AgentCaller(
    private val agentProvider: AgentProvider,
    private val errorHandler: ErrorHandler? = null,
    private val contextHandler: ContextHandler = EmptyContextHandler(),
    private val agentResolver: AgentResolver? = null,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Calls the agent with the given request and data stream.
     */
    fun callAgent(agentName: String? = null, request: AgentRequest, dataStream: DataStream) =
        channelFlow {
            coroutineScope {
                val agent = findAgent(agentName, request)
                val anonymizationEntities =
                    AnonymizationEntities(request.conversationContext.anonymizationEntities.convertConversationEntities())
                val start = System.nanoTime()
                val messageChannel = Channel<AssistantMessage>()

                log.info("Calling Agent $agentName")

                async {
                    sendIntermediateMessage(messageChannel, start, anonymizationEntities)
                }

                val result = contextHandler.inject(request) {
                    withLogContext(agent.name, request) {
                        agent.execute(
                            Conversation(
                                user = request.userContext?.userId?.let { User(it) },
                                conversationId = request.conversationContext.conversationId,
                                currentTurnId = request.conversationContext.turnId
                                    ?: request.messages.lastOrNull()?.turnId
                                    ?: request.messages.size.toString(),
                                transcript = request.messages.convert(dataStream),
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
                            messages = buildOutputMessages(result.value),
                            anonymizationEntities = anonymizationEntities.entities.convertAPIEntities(),
                        ) to result.value.latest<AssistantMessage>()?.binaryData?.firstOrNull()?.stream,
                    )

                    is Failure -> {
                        val handledResult = (errorHandler?.handleError(result.reason) ?: result).getOrThrow()
                        send(
                            AgentResult(
                                responseTime = responseTime,
                                messages = listOf(handledResult.toMessage()),
                                anonymizationEntities = emptyList(),
                            ) to null,
                        )
                    }
                }
                messageChannel.close()
            }
        }

    private fun buildOutputMessages(conversation: Conversation): List<Message> = buildList {
        val assistantMessage = conversation.latest<AssistantMessage>()
        if (!assistantMessage?.userTranscript.isNullOrEmpty()) {
            add(userMessage(assistantMessage?.userTranscript!!, turnId = assistantMessage.turnId))
        }
        add(assistantMessage.toMessage())
    }

    private fun findAgent(agentName: String?, request: AgentRequest): ConversationAgent =
        agentName?.let { agentProvider.getAgentByName(it) } as ConversationAgent?
            ?: agentResolver?.resolveAgent(agentName, request) as ConversationAgent?
            ?: agentProvider.getAgents().firstOrNull() as ConversationAgent?
            ?: error("No Agent defined!")

    private suspend fun ProducerScope<Pair<AgentResult, DataStream?>>.sendIntermediateMessage(
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
                ) to null,
            )
        }
    }
}
