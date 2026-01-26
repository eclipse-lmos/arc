// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.agents

import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.agents
import org.eclipse.lmos.arc.agents.dsl.extensions.getCurrentUseCases
import org.eclipse.lmos.arc.agents.dsl.extensions.local
import org.eclipse.lmos.arc.agents.dsl.extensions.processUseCases
import org.eclipse.lmos.arc.agents.dsl.extensions.time
import org.eclipse.lmos.arc.agents.dsl.get
import org.eclipse.lmos.arc.agents.retry
import org.eclipse.lmos.arc.assistants.support.filters.UnresolvedDetector
import org.eclipse.lmos.arc.assistants.support.filters.UseCaseResponseHandler
import org.eclipse.lmos.arc.assistants.support.usecases.UseCase

fun createAssistantAgent(): ConversationAgent = agents {
    agent {
        name = "assistant_agent"
        filterOutput {
            -"```json"
            -"```"
            +UseCaseResponseHandler()
            if(outputMessage.content.contains("NEXT_USE_CASE")) {
                retry("Detected NEXT_USE_CASE usage, retrying to avoid infinite loop.", max = 10)
            }
            +UnresolvedDetector { "UNRESOLVED" }
        }
        prompt {
            val role = local("role.md")!!
            val useCases = processUseCases(useCases = get<List<UseCase>>(), fallbackLimit = 3)
            val prompt = local("assistant.md")!!
                .replace("\$\$ROLE\$\$", role)
                .replace("\$\$USE_CASES\$\$", useCases)
                .replace("\$\$TIME\$\$", time())
            prompt
        }
    }
}.getAgents().first() as ConversationAgent
