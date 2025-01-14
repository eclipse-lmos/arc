// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl

import org.eclipse.lmos.arc.agents.conversation.ConversationMessage

class NumberFilter : AgentFilter {
    override suspend fun filter(m: ConversationMessage): ConversationMessage {
        return m.update(m.content.replace("1234", "NUMBER"))
    }
}

class DateFilter : AgentFilter {
    override suspend fun filter(m: ConversationMessage): ConversationMessage {
        return m.update(m.content.replace("01.01.2024", "DATE"))
    }
}
