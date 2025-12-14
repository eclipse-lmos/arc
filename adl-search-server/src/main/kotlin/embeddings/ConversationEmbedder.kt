// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.search.embeddings

import dev.langchain4j.model.embedding.EmbeddingModel
import org.eclipse.lmos.adl.server.search.inbound.Message

/**
 * Creates a single embedding for an entire conversation.
 * @param embeddingModel The embedding model to use for creating embeddings.
 * @param textStrategy The strategy for converting messages to text.
 */
class ConversationEmbedder(
    private val embeddingModel: EmbeddingModel,
    private val textStrategy: ConversationToTextStrategy = RolePrefixedStrategy(),
) {
    /**
     * Creates a single embedding representing the entire conversation.
     * @param messages The messages in the conversation.
     * @return The embedding of the conversation.
     */
    suspend fun embed(messages: List<Message>): ConversationEmbedding {
        val text = textStrategy.convert(messages)
        val response = embeddingModel.embed(text)
        return ConversationEmbedding(
            text = text,
            embedding = response.content().vector().map { it.toDouble() },
            messageCount = messages.size,
        )
    }

    /**
     * Creates embeddings for multiple conversations.
     * @param conversations A list of conversations (each is a list of messages).
     * @return The embeddings of all conversations.
     */
    suspend fun embedAll(conversations: List<List<Message>>): List<ConversationEmbedding> {
        return conversations.map { embed(it) }
    }
}

/**
 * Represents an embedding for an entire conversation.
 * @param text The text representation of the conversation.
 * @param embedding The vector representation of the conversation.
 * @param messageCount The number of messages in the conversation.
 * @param labels Labels for categorizing the conversation.
 * @param metadata Additional metadata.
 */
data class ConversationEmbedding(
    val text: String,
    val embedding: List<Double>,
    val messageCount: Int,
    val labels: Set<String> = emptySet(),
    val metadata: Map<String, String> = emptyMap(),
)
