// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.openai.api.inbound

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.eclipse.lmos.arc.agents.AgentProvider
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.agent.executeWithHandover
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.SystemMessage
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.conversation.latest
import org.eclipse.lmos.arc.agents.dsl.extensions.SystemContext
import org.eclipse.lmos.arc.agents.dsl.extensions.SystemContextProvider
import org.eclipse.lmos.arc.agents.dsl.extensions.UserProfile
import org.eclipse.lmos.arc.agents.dsl.extensions.UserProfileProvider
import org.eclipse.lmos.arc.api.AgentRequest
import org.eclipse.lmos.arc.api.ConversationContext
import org.eclipse.lmos.arc.api.ProfileEntry
import org.eclipse.lmos.arc.api.SystemContextEntry
import org.eclipse.lmos.arc.api.UserContext
import org.eclipse.lmos.arc.core.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.*
import org.eclipse.lmos.arc.api.Message as ArcMessage

/**
 * Basic implementation of the OpenAI API for Arc.
 * Not all features are supported at the moment.
 */
@RestController
class OpenAIController(
    private val agentProvider: AgentProvider,
    private val key: String?,
) {
    private val log = LoggerFactory.getLogger(OpenAIController::class.java)

    @PostMapping("/openai/v1/chat/completions")
    suspend fun chatCompletion(
        @RequestHeader headers: MultiValueMap<String, String>,
        @RequestHeader("Authorization", required = false) auth: String,
        @RequestBody requestString: String,
    ): ChatResponse {
        log.info("Received chat completion request for $requestString")
        val request = Json.decodeFromString<OpenAIRequest>(requestString)
        if (key != null && auth != "Bearer $key") throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        val agent = agentProvider.getAgents().firstOrNull() as ConversationAgent
        val conversationId = headers.getFirst("conversation") ?: UUID.randomUUID().toString()
        val turnId = headers.getFirst("turn") ?: "1"
        val userId = headers.getFirst("user")
        val userToken = headers.getFirst("user-token")

        val messages =
            request.messages.map {
                val content =
                    when (it.content) {
                        is JsonPrimitive -> it.content.jsonPrimitive.content
                        is JsonArray ->
                            it.content.jsonArray[0]
                                .jsonObject["text"]
                                ?.jsonPrimitive
                                ?.content ?: ""

                        else -> ""
                    }
                when (it.role) {
                    "user" -> UserMessage(content) to ArcMessage(role = it.role, content = content)
                    "assistant" -> AssistantMessage(content) to ArcMessage(role = it.role, content = content)
                    "system" -> SystemMessage(content) to ArcMessage(role = it.role, content = content)
                    else -> throw IllegalArgumentException("Unsupported role: ${it.role}")
                }
            }

        val agentRequest =
            AgentRequest(
                messages = messages.map { it.second },
                conversationContext =
                ConversationContext(
                    conversationId = conversationId,
                    turnId = turnId,
                    anonymizationEntities = emptyList(),
                ),
                systemContext =
                headers
                    .filter {
                        !it.key.startsWith("profile_") &&
                            !it.key.equals(
                                "Authorization",
                                ignoreCase = true,
                            )
                    }.map {
                        SystemContextEntry(
                            it.key,
                            it.value.firstOrNull() ?: "",
                        )
                    },
                userContext =
                UserContext(
                    userId = userId,
                    userToken = userToken,
                    profile =
                    headers
                        .filter { it.key.startsWith("profile_") }
                        .map {
                            ProfileEntry(
                                it.key.substringAfter("profile_"),
                                it.value.firstOrNull() ?: "",
                            )
                        },
                ),
            )

        val result =
            agent
                .executeWithHandover(
                    Conversation(
                        user = null,
                        conversationId = conversationId,
                        currentTurnId = turnId,
                        transcript = messages.map { it.first },
                    ),
                    setOf(
                        ContextProvider(agentRequest),
                        agentRequest,
                    ),
                    agentProvider,
                ).getOrThrow()

        // TODO
        return ChatResponse(
            id = conversationId,
            obj = "chat.completion",
            created = System.currentTimeMillis() / 1000,
            model = request.model,
            choices =
            listOf(
                Choice(
                    index = 0,
                    message =
                    OutMessage(
                        role = "assistant",
                        content = result.latest<AssistantMessage>()?.content ?: "",
                        refusal = null,
                        annotations = emptyList(),
                    ),
                    logprobs = null,
                    finishReason = "stop",
                ),
            ),
            usage =
            Usage(
                promptTokens = -1,
                completionTokens = -1,
                totalTokens = -1,
                promptTokensDetails = PromptTokensDetails(),
                completionTokensDetails = CompletionTokensDetails(),
            ),
            serviceTier = "default",
        )
    }
}

/**
 * Provides the system context and user profile to the context of the DSL.
 */
data class ContextProvider(val request: AgentRequest) : SystemContextProvider, UserProfileProvider {

    override fun provideSystem(): SystemContext {
        return SystemContext(request.systemContext.associate { it.key to it.value })
    }

    override fun provideProfile(): UserProfile {
        return UserProfile(request.userContext.profile.associate { it.key to it.value })
    }
}
