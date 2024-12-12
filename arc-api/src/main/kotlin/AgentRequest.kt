// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.api

import kotlinx.serialization.Serializable

/**
 * AgentRequest
 */
@Serializable
data class AgentRequest(
    val messages: List<Message>,
    val conversationContext: ConversationContext,
    val systemContext: List<SystemContextEntry>,
    val userContext: UserContext,
)

@Serializable
data class UserContext(
    val userId: String? = null,
    val userToken: String? = null,
    val profile: List<ProfileEntry>,
)

@Serializable
data class ConversationContext(
    val conversationId: String,
    val turnId: String? = null,
    val anonymizationEntities: List<AnonymizationEntity>? = null,
)

@Serializable
data class ProfileEntry(
    val key: String,
    val value: String,
)

@Serializable
data class SystemContextEntry(
    val key: String,
    val value: String,
)
