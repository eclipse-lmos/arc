// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.api

import kotlinx.serialization.Serializable

/**
 * AgentResult
 */
@Serializable
data class AgentResult(
    val status: String? = null,
    val responseTime: Double = -1.0,
    val messages: List<Message>,
    val anonymizationEntities: List<AnonymizationEntity>? = null,
    val context: List<ContextEntry>? = null,
    val toolCalls: List<ToolCall>? = null,
)

/**
 * Represents a tool call made by the LLM Model.
 */
@Serializable
data class ToolCall(val name: String, val arguments: String)

/**
 * Context contain entries that were added during the processing of the request.
 * For example, the context can contain data that was extracted from the user's input,
 * for example, the user's name.
 * Can also contain self-evaluation data, for example, the confidence of the agent in the response.
 */
@Serializable
data class ContextEntry(
    val key: String,
    val value: String,
)
