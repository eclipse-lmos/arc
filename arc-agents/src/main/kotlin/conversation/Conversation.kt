// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.conversation

import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.agents.User
import java.util.*

@Serializable
data class Conversation(
    val user: User? = null,
    val conversationId: String = UUID.randomUUID().toString(),
    val classification: ConversationClassification? = null,
    val currentTurnId: String = UUID.randomUUID().toString(),
    val transcript: List<ConversationMessage> = emptyList(),
    val anonymizationEntities: List<AnonymizationEntity> = emptyList(),
) {

    fun isEmpty() = transcript.isEmpty()

    /**
     * Returns the AssistantMessage corresponding to the turn id.
     */
    fun getAssistantMessage(turnId: String) = transcript.firstOrNull { it is AssistantMessage && it.turnId == turnId }

    /**
     * Returns a new instance of the Conversation with the message appended to the end of the history.
     * The turnId of the Conversation is automatically applied.
     */
    fun add(message: ConversationMessage) =
        copy(transcript = transcript + message.applyTurn(currentTurnId))

    /**
     * Returns a new instance of the Conversation with the message inserted at the front of the history.
     * The turnId of the Conversation is automatically applied.
     */
    fun addFirst(message: ConversationMessage) =
        copy(transcript = listOf(message.applyTurn(currentTurnId)) + transcript)

    /**
     * Returns a new instance of the Conversation with the message added to the history.
     * If the message is a SystemMessage, it is added in the front otherwise the message is appended.
     * The turnId of the Conversation is automatically applied.
     */
    operator fun plus(message: ConversationMessage) = if (message is SystemMessage) addFirst(message) else add(message)

    /**
     * Returns true if one or more messages in the conversation history contain sensitive data.
     */
    fun hasSensitiveMessages() = transcript.any { it.sensitive }

    /**
     * Returns a new instance of the conversation with the filter applied to each message.
     * The filter can remove a message by returning null.
     */
    suspend fun map(filter: suspend (ConversationMessage) -> ConversationMessage?) =
        copy(transcript = transcript.mapNotNull { filter(it) })

    /**
     * Returns a new instance of the conversation with the filter applied to the latest message.
     * The filter can remove a message by returning null.
     */
    suspend fun mapLatest(filter: suspend (ConversationMessage) -> ConversationMessage?) =
        copy(transcript = transcript.mapIndexedNotNull { i, m -> if (i == (transcript.size - 1)) filter(m) else m })

    /**
     * Returns a new instance of the conversation with the last message removed.
     */
    fun removeLast() = copy(transcript = transcript.dropLast(1))
}

inline fun <reified T : ConversationMessage> Conversation.latest(): T? = transcript.findLast { it is T } as T?

/**
 * Defined a classification for a conversation.
 */
interface ConversationClassification

@Serializable
data class AnonymizationEntity(val type: String, val value: String, val replacement: String)

/**
 * Converts a string to a conversation object.
 */
fun String.toConversation(user: User) = Conversation(user = user, transcript = listOf(UserMessage(this)))
