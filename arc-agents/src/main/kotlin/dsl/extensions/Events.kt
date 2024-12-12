// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.get
import org.eclipse.lmos.arc.agents.events.Event
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.events.MessagePublisher

/**
 * Extensions for eventing.
 */
suspend fun DSLContext.emit(event: Event) {
    get<EventPublisher>().publish(event)
}

/**
 * Emits a message to the client.
 */
suspend fun DSLContext.emitMessage(msg: String) {
    val currentTurnId = get<Conversation>().currentTurnId
    emitMessage(AssistantMessage(msg, turnId = currentTurnId))
}

suspend fun DSLContext.emitMessage(msg: AssistantMessage) {
    get<MessagePublisher>().publish(msg)
}
