// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.search.embeddings

import org.eclipse.lmos.adl.server.search.inbound.Message

/**
 * Strategy for converting a list of messages into text for embedding.
 */
fun interface ConversationToTextStrategy {

    /**
     * Converts a list of messages to text suitable for embedding.
     * @param messages The messages to convert.
     * @return The text representation of the messages.
     */
    fun convert(messages: List<Message>): String
}

/**
 * Concatenates all message contents into a single text.
 */
class ConcatenatingStrategy(
    private val separator: String = "\n",
) : ConversationToTextStrategy {
    override fun convert(messages: List<Message>): String {
        return messages.joinToString(separator) { it.content }
    }
}

/**
 * Includes role prefixes in the concatenated text.
 */
class RolePrefixedStrategy(
    private val separator: String = "\n",
) : ConversationToTextStrategy {
    override fun convert(messages: List<Message>): String {
        return messages.joinToString(separator) { "${it.role}: ${it.content}" }
    }
}

/**
 * Uses only the last N messages for embedding.
 */
class LastNMessagesStrategy(
    private val n: Int,
    private val delegate: ConversationToTextStrategy = ConcatenatingStrategy(),
) : ConversationToTextStrategy {
    override fun convert(messages: List<Message>): String {
        return delegate.convert(messages.takeLast(n))
    }
}

/**
 * Filters messages by role before converting.
 */
class FilterByRoleStrategy(
    private val roles: Set<String>,
    private val delegate: ConversationToTextStrategy = ConcatenatingStrategy(),
) : ConversationToTextStrategy {
    override fun convert(messages: List<Message>): String {
        return delegate.convert(messages.filter { it.role in roles })
    }
}
