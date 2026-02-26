// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.models

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import kotlinx.serialization.Serializable

/**
 * Represents a turn in a conversation.
 */
@Serializable
data class ConversationTurn(
    @GraphQLDescription("The role of the speaker (e.g., user, assistant)")
    val role: String,
    @GraphQLDescription("The content of the message")
    val content: String,
)
