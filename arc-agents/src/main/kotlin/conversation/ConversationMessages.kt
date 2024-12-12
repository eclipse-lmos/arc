// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.conversation

import kotlinx.serialization.Serializable

/**
 * Conversation Messages.
 */
@Serializable
sealed class ConversationMessage {

    abstract val turnId: String?
    abstract val content: String
    abstract val sensitive: Boolean
    abstract val anonymized: Boolean
    abstract val binaryData: List<BinaryData>
    abstract val format: MessageFormat

    /**
     * Returns a new instance of the message with the turn id applied.
     * The turn id is normally add when a message is added to a conversation.
     */
    abstract fun applyTurn(turnId: String): ConversationMessage

    /**
     * Returns a new instance of the message with the content updated.
     */
    abstract fun update(content: String): ConversationMessage
}

/**
 * A message sent by the user.
 */
@Serializable
data class UserMessage(
    override val content: String,
    override val turnId: String? = null,
    override val sensitive: Boolean = false,
    override val anonymized: Boolean = false,
    override val binaryData: List<BinaryData> = emptyList(),
    override val format: MessageFormat = MessageFormat.TEXT,
) : ConversationMessage() {
    override fun applyTurn(turnId: String): UserMessage = copy(turnId = turnId)
    override fun update(content: String): UserMessage = copy(content = content)
}

/**
 * Creates a user message with binary data.
 */
fun binaryUserMessage(binaryData: BinaryData) = UserMessage(content = "", binaryData = listOf(binaryData))

/**
 * A message added by the Platform to help instruct or guide the LLM.
 */
@Serializable
data class SystemMessage(
    override val content: String,
    override val turnId: String? = null,
    override val sensitive: Boolean = false,
    override val anonymized: Boolean = false,
    override val binaryData: List<BinaryData> = emptyList(),
    override val format: MessageFormat = MessageFormat.TEXT,
) : ConversationMessage() {
    override fun applyTurn(turnId: String): SystemMessage = copy(turnId = turnId)
    override fun update(content: String): SystemMessage = copy(content = content)
}

/**
 * A message sent by the LLM Model.
 */
@Serializable
data class AssistantMessage(
    override val content: String,
    override val turnId: String? = null,
    override val sensitive: Boolean = false,
    override val anonymized: Boolean = false,
    override val binaryData: List<BinaryData> = emptyList(),
    override val format: MessageFormat = MessageFormat.TEXT,
) : ConversationMessage() {
    override fun applyTurn(turnId: String): AssistantMessage = copy(turnId = turnId)
    override fun update(content: String): AssistantMessage = copy(content = content)
}

@Serializable
class BinaryData(val mimeType: String, val data: ByteArray)

enum class MessageFormat {
    JSON,
    TEXT,
    BINARY,
}
