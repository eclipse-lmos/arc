// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.agents

import org.eclipse.lmos.adl.server.agents.extensions.conversationGuide
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.agents
import org.eclipse.lmos.arc.agents.dsl.extensions.getCurrentUseCases
import org.eclipse.lmos.arc.agents.dsl.extensions.local
import org.eclipse.lmos.arc.agents.dsl.extensions.processUseCases
import org.eclipse.lmos.arc.agents.dsl.extensions.time
import org.eclipse.lmos.arc.agents.dsl.get
import org.eclipse.lmos.arc.agents.events.LoggingEventHandler
import org.eclipse.lmos.arc.agents.retry
import org.eclipse.lmos.arc.assistants.support.filters.UnresolvedDetector
import org.eclipse.lmos.arc.assistants.support.filters.UseCaseResponseHandler
import org.eclipse.lmos.arc.assistants.support.usecases.Conditional
import org.eclipse.lmos.arc.assistants.support.usecases.UseCase

fun createAssistantAgent(): ConversationAgent = agents(
    handlers = listOf(LoggingEventHandler())
) {
    agent {
        name = "assistant_agent"
        filterOutput {
            -"```json"
            -"```"
            +UseCaseResponseHandler()
            +UnresolvedDetector { "UNRESOLVED" }
        }
        conversationGuide()
        prompt {
            val role = local("role.md")!!

            // Convert steps to conditionals in use cases
            val useCases = get<List<UseCase>>().map { uc ->
                if (uc.steps.isNotEmpty()) {
                    val convertedSteps = uc.steps.filter { it.text.isNotEmpty() }.mapIndexed { i, step ->
                        step.copy(conditions = step.conditions + "step_${i + 1}")
                    }
                    uc.copy(solution = convertedSteps + uc.solution.map { s ->
                        s.copy(conditions = s.conditions + "else")
                    }, steps = emptyList())
                } else uc
            }
            val useCasesPrompt = processUseCases(useCases = useCases, fallbackLimit = 3)

            // Output the final prompt
            val prompt = local("assistant.md")!!
                .replace("\$\$ROLE\$\$", role)
                .replace("\$\$USE_CASES\$\$", useCasesPrompt)
                .replace("\$\$TIME\$\$", time())
            prompt
        }
    }
}.getAgents().first() as ConversationAgent
