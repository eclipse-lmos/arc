// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.extensions.MemoryScope.LONG_TERM
import org.eclipse.lmos.arc.agents.dsl.extensions.MemoryScope.SHORT_TERM
import org.eclipse.lmos.arc.agents.dsl.get
import org.eclipse.lmos.arc.agents.memory.Memory

enum class MemoryScope {
    SHORT_TERM,
    LONG_TERM,
}

/**
 * Extensions that add memory functions to the DSL context.
 */
suspend fun DSLContext.memory(key: String, value: Any?, scope: MemoryScope = SHORT_TERM) {
    val conversation = get<Conversation>()
    val memory = get<Memory>()

    if (LONG_TERM == scope && conversation.user?.id == null) {
        kotlin.error("LONG_TERM is only available when a user id is present!")
    }

    val owner = conversation.user?.id ?: conversation.conversationId
    when (scope) {
        SHORT_TERM -> memory.storeShortTerm(owner, key, value, conversation.conversationId)
        LONG_TERM -> memory.storeLongTerm(owner, key, value)
    }
}

suspend fun DSLContext.memory(key: String): Any? {
    val conversation = get<Conversation>()
    val memory = get<Memory>()
    val owner = conversation.user?.id ?: conversation.conversationId
    return memory.fetch(owner, key, conversation.conversationId)
}
