// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.events

import kotlinx.coroutines.channels.Channel
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage

/**
 * Publishes a message that is returned to the client.
 */
fun interface MessagePublisher {
    suspend fun publish(message: AssistantMessage)
}

fun interface MessageChannel {
    fun publish(message: AssistantMessage)
}

class MessagePublisherChannel(val channel: Channel<AssistantMessage>) : MessagePublisher {

    override suspend fun publish(message: AssistantMessage) {
        channel.trySend(message)
    }
}
