// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.agent

import kotlinx.serialization.json.Json
import org.eclipse.lmos.arc.agents.AgentFailedException
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.User
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.latest
import org.eclipse.lmos.arc.agents.conversation.toConversation
import org.eclipse.lmos.arc.core.failWith
import org.eclipse.lmos.arc.core.map
import org.eclipse.lmos.arc.core.result

/**
 * Helper function to "ask" the agent a question.
 *
 * @param question The question to ask the agent.
 * @param user Optional user context for the conversation.
 * @return A Result containing the conversation or an AgentFailedException.
 */
suspend fun ConversationAgent.ask(question: String, user: User? = null) = result<String, AgentFailedException> {
    return execute(question.toConversation(user)).map { it.latest<AssistantMessage>()?.content ?: "" }
}


/**
 * JSON serializer/deserializer for agent input and output.
 */
val agentInputJson = Json { encodeDefaults = true; ignoreUnknownKeys = true }

/**
 * Processes the input through the agent and returns the result deserialized to the specified type.
 *
 * @param R The expected return type.
 * @param input The input to process.
 * @return A Result containing the processed output of type R or an AgentFailedException.
 */
suspend inline fun <reified T, reified R> ConversationAgent.process(input: T) = result<R, AgentFailedException> {
    val inputString = agentInputJson.encodeToString<T>(input)
    val result = ask(inputString) failWith { it }
    agentInputJson.decodeFromString(result)
}
