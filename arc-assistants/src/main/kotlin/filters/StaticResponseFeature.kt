// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.filters

import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.dsl.AgentOutputFilter
import org.eclipse.lmos.arc.agents.dsl.OutputFilterContext
import org.eclipse.lmos.arc.agents.dsl.extensions.getCurrentUseCases
import org.eclipse.lmos.arc.assistants.support.usecases.toUseCases


class StaticResponseFeature : AgentOutputFilter {

    override suspend fun filter(
        message: ConversationMessage,
        context: OutputFilterContext
    ): ConversationMessage {
        val currentUseCaseId = context.getCurrentUseCases()?.currentUseCaseId
        context.getCurrentUseCases()?.processedUseCaseMap?.get(currentUseCaseId)?.let { uc ->
            val solution = uc.toUseCases().first().solution.joinToString("\n").trim()
            if ((solution.startsWith("\"") || solution.startsWith("- \"")) && solution.endsWith("\"")) {
                return message.update(solution.substringAfter("\"").substringBeforeLast("\""))
            }
        }
        return message
    }
}
