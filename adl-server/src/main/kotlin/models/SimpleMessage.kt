package org.eclipse.lmos.adl.server.model

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import kotlinx.serialization.Serializable

/**
 * Represents a message in a conversation.
 * @param role The role of the message sender (e.g., "user", "assistant", "system").
 * @param content The content of the message.
 */
@Serializable
data class SimpleMessage(
    @param:GraphQLDescription("The role of the message sender (e.g., user, assistant, system)")
    val role: String,
    @param:GraphQLDescription("The content of the message")
    val content: String,
)