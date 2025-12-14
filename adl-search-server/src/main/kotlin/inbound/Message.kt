// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.search.inbound

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import kotlinx.serialization.Serializable

/**
 * Represents a message in a conversation.
 * @param role The role of the message sender (e.g., "user", "assistant", "system").
 * @param content The content of the message.
 */
@Serializable
data class Message(
    @param:GraphQLDescription("The role of the message sender (e.g., user, assistant, system)")
    val role: String,
    @param:GraphQLDescription("The content of the message")
    val content: String,
)
