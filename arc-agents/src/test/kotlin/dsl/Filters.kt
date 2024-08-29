// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package ai.ancf.lmos.arc.agents.dsl

import ai.ancf.lmos.arc.agents.conversation.ConversationMessage

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
