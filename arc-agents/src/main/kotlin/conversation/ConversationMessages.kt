// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.conversation

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.reduce
import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

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
 * Returns a loggable string representation of the message.
 */
fun List<ConversationMessage>.toLogString(): String {
    return joinToString("\n") { message ->
        when (message) {
            is UserMessage -> "User:[${message.content}]"
            is SystemMessage -> "System:[${message.content}]"
            is AssistantMessage -> "Assistant:[${message.content}]"
            is DeveloperMessage -> "Developer:[${message.content}]"
        }
    }
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
    val toolCalls: List<ToolCall>? = null,
    val userTranscript: String? = null,
    val last: Boolean = true,
) : ConversationMessage() {
    override fun applyTurn(turnId: String): AssistantMessage = copy(turnId = turnId)
    override fun update(content: String): AssistantMessage = copy(content = content)
}

/**
 * Represents a tool call made by the LLM Model.
 */
@Serializable
data class ToolCall(val name: String, val arguments: String)

/**
 * A message sent by the Developer.
 */
@Serializable
data class DeveloperMessage(
    override val content: String,
    override val turnId: String? = null,
    override val sensitive: Boolean = false,
    override val anonymized: Boolean = false,
    override val binaryData: List<BinaryData> = emptyList(),
    override val format: MessageFormat = MessageFormat.TEXT,
) : ConversationMessage() {
    override fun applyTurn(turnId: String): DeveloperMessage = copy(turnId = turnId)
    override fun update(content: String): DeveloperMessage = copy(content = content)
}

@Serializable
class BinaryData(val mimeType: String, val stream: DataStream) {

    /**
     * Reads all bytes from stream.
     */
    suspend fun readAllBytes(): ByteArray = stream.readAllBytes()
}

enum class MessageFormat {
    JSON,
    TEXT,
    BINARY,
}

/**
 * Represents a data stream used to stream binary data of a Conversation Message.
 */
interface DataStream {
    fun stream(): Flow<ByteArray>
}

/**
 * Reads all bytes from stream.
 */
suspend fun DataStream.readAllBytes(): ByteArray = stream().reduce { acc, bytes -> acc + bytes }

/**
 * A data stream that reads data from a base64 encoded string.
 */
class Base64DataStream(dataAsBase64: String) : DataStream {

    @OptIn(ExperimentalEncodingApi::class)
    private val data = Base64.decode(dataAsBase64)

    override fun stream(): Flow<ByteArray> = flow { emit(data) }
}

fun String.asDataStream() = Base64DataStream(this)

/**
 * A data stream that can be written to.
 */
class WritableDataStream : DataStream {
    private val channel = Channel<ByteArray>(capacity = Channel.UNLIMITED)

    override fun stream(): Flow<ByteArray> = channel.receiveAsFlow()

    fun write(data: ByteArray) {
        channel.trySend(data)
    }

    fun close() {
        channel.close()
    }
}

/**
 * A data stream that holds a ByteArray
 */
class ArrayDataStream(data: ByteArray? = null) : DataStream {

    private val dataHolder = AtomicReference<ByteArray>(data)

    override fun stream(): Flow<ByteArray> = flow { emit(dataHolder.get()) }
}

fun ByteArray.asDataStream() = ArrayDataStream(this)
