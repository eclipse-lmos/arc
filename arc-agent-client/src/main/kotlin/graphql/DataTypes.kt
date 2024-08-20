// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package io.github.lmos.arc.agent.client.graphql

import io.github.lmos.arc.api.AgentRequest
import io.github.lmos.arc.api.AnonymizationEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Message types defined by the graphql-ws Specification.
 * See https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md
 */

/**
 * Client messages
 */
@Serializable
sealed interface ClientMessage

@Serializable
@SerialName("connection_init")
data object InitConnectionMessage : ClientMessage

@Serializable
@SerialName("subscribe")
data class SubscribeMessage(val id: String, val payload: ClientPayload) : ClientMessage

@Serializable
data class ClientPayload(val query: String, val variables: AgentRequestVariables)

@Serializable
data class AgentRequestVariables(val request: AgentRequest)

/**
 * Server messages
 */
@Serializable
sealed interface ServerMessage

@Serializable
@SerialName("connection_ack")
data object AckConnectionMessage : ServerMessage

@Serializable
@SerialName("next")
data class NextMessage(val id: String, val payload: ServerPayload) : ServerMessage

@Serializable
data class ServerPayload(val data: DataPayload)

@Serializable
data class DataPayload(val agent: AgentPayload)

@Serializable
data class AgentPayload(
    val anonymizationEntities: List<AnonymizationEntity>,
    val messages: List<AgentPayloadMessage>,
)

@Serializable
data class AgentPayloadMessage(val content: String)

@Serializable
@SerialName("complete")
data class CompleteMessage(val id: String) : ServerMessage

@Serializable
@SerialName("error")
data class ErrorMessage(val id: String, val payload: List<ErrorPayloadMessage>) : ServerMessage

@Serializable
data class ErrorPayloadMessage(val message: String)
