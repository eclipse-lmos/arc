// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.Data
import org.eclipse.lmos.arc.agents.dsl.addData
import org.eclipse.lmos.arc.agents.dsl.extensions.MemoryScope.LONG_TERM
import org.eclipse.lmos.arc.agents.dsl.extensions.MemoryScope.SHORT_TERM
import org.eclipse.lmos.arc.agents.dsl.get
import org.eclipse.lmos.arc.agents.dsl.getOptional
import org.eclipse.lmos.arc.agents.events.BaseEvent
import org.eclipse.lmos.arc.agents.events.Event
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.memory.Memory

enum class MemoryScope {
    SHORT_TERM,
    LONG_TERM,
}

/**
 * Extensions that add memory functions to the DSL context.
 */
suspend fun DSLContext.memory(key: String, value: Any?, scope: MemoryScope = SHORT_TERM, loggable: Boolean = true) {
    val conversation = get<Conversation>()
    val memory = get<Memory>()

    getOptional<EventPublisher>()?.publish(MemoryStoreEvent(key, if (loggable) value else "****", scope))

    if (LONG_TERM == scope && conversation.user?.id == null) {
        kotlin.error("LONG_TERM is only available when a user id is present!")
    }

    val owner = conversation.user?.id ?: conversation.conversationId
    when (scope) {
        SHORT_TERM -> memory.storeShortTerm(owner, key, value, conversation.conversationId)
        LONG_TERM -> memory.storeLongTerm(owner, key, value)
    }
}

suspend fun <T> DSLContext.memory(key: String): T? {
    val conversation = get<Conversation>()
    val memory = get<Memory>()
    val owner = conversation.user?.id ?: conversation.conversationId
    getOptional<EventPublisher>()?.publish(MemoryRetrieveEvent(key))
    return memory.fetch<T?>(owner, key, conversation.conversationId).also {
        addData(Data(key, it.toString()))
    }
}

/**
 * Published when a memory operation is performed.
 */
sealed class MemoryEvent : Event by BaseEvent()

class MemoryStoreEvent(
    val key: String,
    val value: Any?,
    val scope: MemoryScope,
) : MemoryEvent()

class MemoryRetrieveEvent(val key: String) : MemoryEvent()
