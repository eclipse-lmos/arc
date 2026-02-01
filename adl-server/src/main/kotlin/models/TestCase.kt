// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a generated test case.
 */
@Serializable
data class TestCase(
    val id: String = java.util.UUID.randomUUID().toString(),
    val useCaseId: String? = null,
    val adlId : String? = null,
    val name: String,
    val description: String,
    @SerialName("expected_conversation")
    val expectedConversation: List<ConversationTurn>,
    val variants : List<List<ConversationTurn>> = emptyList(),
    val contract: Boolean = false
)
