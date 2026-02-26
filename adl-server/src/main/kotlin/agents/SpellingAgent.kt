// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.agents

import org.eclipse.lmos.adl.server.agents.extensions.ConversationGuider
import org.eclipse.lmos.adl.server.agents.extensions.InputHintProvider
import org.eclipse.lmos.adl.server.agents.extensions.currentDate
import org.eclipse.lmos.adl.server.agents.extensions.isWeekend
import org.eclipse.lmos.adl.server.repositories.AdlRepository
import org.eclipse.lmos.adl.server.repositories.TestCaseRepository
import org.eclipse.lmos.adl.server.repositories.UseCaseEmbeddingsRepository
import org.eclipse.lmos.adl.server.services.McpService
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.agents
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.conversation.latest
import org.eclipse.lmos.arc.agents.dsl.extensions.addTool
import org.eclipse.lmos.arc.agents.dsl.extensions.getCurrentUseCases
import org.eclipse.lmos.arc.agents.dsl.extensions.info
import org.eclipse.lmos.arc.agents.dsl.extensions.local
import org.eclipse.lmos.arc.agents.dsl.extensions.processUseCases
import org.eclipse.lmos.arc.agents.dsl.extensions.time
import org.eclipse.lmos.arc.agents.dsl.get
import org.eclipse.lmos.arc.agents.events.LoggingEventHandler
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.assistants.support.filters.UnresolvedDetector
import org.eclipse.lmos.arc.assistants.support.filters.UseCaseResponseHandler
import org.eclipse.lmos.arc.assistants.support.usecases.UseCase
import org.eclipse.lmos.arc.assistants.support.usecases.toUseCases

/**
 * Creates the agent that checks for spelling and grammar errors.
 */
fun createSpellingAgent(chatCompleterProvider: ChatCompleterProvider? = null): ConversationAgent = agents(chatCompleterProvider = chatCompleterProvider) {
    agent {
        name = "spelling_agent"
        settings = { ChatCompletionSettings(temperature = 0.0, seed = 42) }
        filterOutput {
            -"```json"
            -"```"
            "UseCase" replaces "Use Case"
        }
        prompt {
           """
               You are a grammar and spelling correction tool.
               Fix spelling and grammar errors in the input text.
               Preserve the original wording, structure, and formatting as much as possible.
               Return only the corrected input text.
               Do not add explanations, comments, or extra text.
           """.trimIndent()
        }
    }
}.getAgents().first() as ConversationAgent
