// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.dsl.AgentFilter
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.InputFilterContext
import org.eclipse.lmos.arc.agents.dsl.OutputFilterContext
import org.eclipse.lmos.arc.agents.dsl.get
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Ensure that the agent is only called with a certain percent otherwise break with the given value.
 */
suspend fun InputFilterContext.admit(percent: Int, returnValue: String) {
    +AdmitFilter(percent, returnValue)
}

suspend fun OutputFilterContext.admit(percent: Int, returnValue: String) {
    +AdmitFilter(percent, returnValue)
}

/**
 * Global map to keep track of conversation turns.
 */
private val conversations = ConcurrentHashMap<String, ConversationEntry>()

context(DSLContext)
class AdmitFilter(
    percent: Int,
    private val returnValue: String,
    private val maxSize: Int = 1000,
    private val maxAge: Duration = 5.minutes,
) :
    AgentFilter {

    init {
        require(percent in 1..100) { "Percent must be between 1 and 100" }
    }

    private val log = LoggerFactory.getLogger(javaClass)
    private val count = 100 / percent

    override suspend fun filter(message: ConversationMessage): ConversationMessage {
        val conversationId = get<Conversation>().conversationId
        val accepted = conversations.computeIfAbsent(conversationId) {
            val accepted = ((conversations.size + 1) % count) == 0
            ConversationEntry(Instant.now(), accepted)
        }.accepted

        cleanUp()

        if (!accepted) {
            breakWith(returnValue)
        }
        return message
    }

    /**
     * Ensure that the map does not grow indefinitely.
     */
    private fun cleanUp() {
        val now = Instant.now()
        conversations.entries.removeIf { it.value.createdAt.isBefore(now.minus(maxAge.toJavaDuration())) }
        if (conversations.entries.size > maxSize) {
            log.warn("Max size of AdmitFilter cache ($maxSize) reached. Cleaning up...")
            conversations.entries.removeIf { it.value.createdAt.isBefore(now.minus(10.seconds.toJavaDuration())) }
        }
    }
}

private class ConversationEntry(val createdAt: Instant, val accepted: Boolean)
