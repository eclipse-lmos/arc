// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.graphql.inbound

import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.api.AnonymizationEntity
import org.eclipse.lmos.arc.api.Message

fun List<Message>.convert() = map {
    when (it.role) {
        "user" -> UserMessage(it.content)
        "assistant" -> AssistantMessage(it.content)
        else -> throw IllegalArgumentException("Unknown role: ${it.role}")
    }
}

fun AssistantMessage?.toMessage() = Message("assistant", this?.content ?: "", turnId = this?.turnId)

fun List<AnonymizationEntity>?.convertConversationEntities() = this?.map {
    org.eclipse.lmos.arc.agents.conversation.AnonymizationEntity(
        type = it.type,
        value = it.value,
        replacement = it.replacement,
    )
} ?: emptyList()

fun List<org.eclipse.lmos.arc.agents.conversation.AnonymizationEntity>?.convertAPIEntities() = this?.map {
    AnonymizationEntity(
        type = it.type,
        value = it.value,
        replacement = it.replacement,
    )
} ?: emptyList()
