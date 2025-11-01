// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.assistants.support.filters

import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.dsl.AgentOutputFilter
import org.eclipse.lmos.arc.agents.dsl.OutputFilterContext
import org.eclipse.lmos.arc.agents.dsl.extensions.getCurrentUseCases
import org.eclipse.lmos.arc.agents.dsl.extensions.system
import org.slf4j.LoggerFactory

/**
 * Returns the use case id if the system property "returnUseCaseId" is set to true.
 * This will only work for Agents that use the useCase function.
 */
class ReturnUseCaseIdFilter : AgentOutputFilter {
    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun filter(message: ConversationMessage, context: OutputFilterContext): ConversationMessage {
        with(context) {
            if (system("returnUseCaseId", "false") == "true") {
                getCurrentUseCases()?.currentUseCaseId?.let {
                    log.debug("Returning use case id: $it")
                    return message.update("<USE_CASE:$it>${message.content}")
                }
            }
        }
        return message
    }
}
