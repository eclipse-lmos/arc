// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.inbound.rest

import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.eclipse.lmos.adl.server.models.OpenAIChatCompletionRequest
import org.eclipse.lmos.adl.server.models.OpenAIChatCompletionResponse
import org.eclipse.lmos.adl.server.models.OpenAIChoice
import org.eclipse.lmos.adl.server.models.OpenAIMessage
import org.eclipse.lmos.adl.server.models.OpenAIUsage
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.SystemMessage
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Success
import java.util.UUID

fun Route.openAICompletions(assistantAgent: ConversationAgent) {
    route("/v1/chat/completions") {
        post {
            val request = call.receive<OpenAIChatCompletionRequest>()

            val messages = request.messages.map { msg ->
                when (msg.role) {
                    "system" -> SystemMessage(content = msg.content)
                    "user" -> UserMessage(content = msg.content)
                    "assistant" -> AssistantMessage(content = msg.content)
                    else -> UserMessage(content = msg.content)
                }
            }

            val conversationId = call.request.headers["X-Conversation-Id"]?.takeIf { it.isNotBlank() }
                ?: UUID.randomUUID().toString()

            // Create initial conversation from messages
            val initialConversation = Conversation(
                conversationId = conversationId,
                transcript = messages
            )

            val result = assistantAgent.execute(initialConversation, emptySet())

            val conversation = when (result) {
                 is Success -> result.value
                 is Failure -> throw RuntimeException("Agent execution failed: ${result.reason}")
            }

            val lastMessage = conversation.transcript.lastOrNull()
            val content = lastMessage?.content ?: ""

            val response = OpenAIChatCompletionResponse(
                id = "chatcmpl-${UUID.randomUUID()}",
                `object` = "chat.completion",
                created = System.currentTimeMillis() / 1000,
                model = request.model,
                choices = listOf(
                    OpenAIChoice(
                        index = 0,
                        message = OpenAIMessage(
                            role = "assistant",
                            content = content
                        ),
                        finish_reason = "stop"
                    )
                ),
                usage = OpenAIUsage(0, 0, 0)
            )
            call.response.headers.append("X-Conversation-Id", conversationId)
            call.respond(response)
        }
    }
}
