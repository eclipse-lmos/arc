// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.agents

import ai.djl.repository.Repository
import org.eclipse.lmos.adl.server.agents.extensions.ConversationGuider
import org.eclipse.lmos.adl.server.agents.extensions.InputHintProvider
import org.eclipse.lmos.adl.server.agents.extensions.currentDate
import org.eclipse.lmos.adl.server.agents.extensions.isWeekend
import org.eclipse.lmos.adl.server.repositories.TestCaseRepository
import org.eclipse.lmos.adl.server.services.McpService
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.agents
import org.eclipse.lmos.arc.agents.dsl.extensions.addTool
import org.eclipse.lmos.arc.agents.dsl.extensions.getCurrentUseCases
import org.eclipse.lmos.arc.agents.dsl.extensions.local
import org.eclipse.lmos.arc.agents.dsl.extensions.processUseCases
import org.eclipse.lmos.arc.agents.dsl.extensions.time
import org.eclipse.lmos.arc.agents.dsl.get
import org.eclipse.lmos.arc.agents.events.LoggingEventHandler
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.assistants.support.filters.UnresolvedDetector
import org.eclipse.lmos.arc.assistants.support.filters.UseCaseResponseHandler
import org.eclipse.lmos.arc.assistants.support.usecases.UseCase
import org.eclipse.lmos.arc.assistants.support.usecases.toUseCases

/**
 * Creates and configures the main Assistant Agent for the ADL server.
 *
 * This agent is responsible for handling user interactions, processing use cases,
 * and integrating with MCP tools. It sets up:
 * - Input/Output filters for handling hints and response formatting.
 * - Prompt generation logic that incorporates roles, use cases, and time context.
 * - specialized handling for step-based use cases by converting them to conditional logic.
 *
 * @param mcpService The service responsible for loading and managing MCP tools.
 * @return A configured [ConversationAgent] ready to handle requests.
 */
fun createAssistantAgent(mcpService: McpService, testRepository: TestCaseRepository): ConversationAgent = agents(
    handlers = listOf(LoggingEventHandler()),
    functionLoaders = listOf(mcpService)
) {
    agent {
        name = "assistant_agent"
        settings = { ChatCompletionSettings(temperature = 0.0, seed = 42) }
        filterInput {
            +InputHintProvider()
        }
        filterOutput {
            -"```json"
            -"```"
            +UseCaseResponseHandler()
            +ConversationGuider()
            getCurrentUseCases()?.processedUseCaseMap?.get(getCurrentUseCases()?.currentUseCaseId)?.let { uc ->
                val solution = uc.toUseCases().first().solution.joinToString("\n").trim()
                if ((solution.startsWith("\"") || solution.startsWith("- \"")) && solution.endsWith("\"")) {
                    outputMessage = outputMessage.update(solution.substringAfter("\"").substringBeforeLast("\""))
                }
            }
            +UnresolvedDetector { "UNRESOLVED" }
        }
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

            // Add examples from the test repository
            val examples = buildString {
                append("## Examples:\n")
                useCases.forEach { uc ->
                    testRepository.findByUseCaseId(uc.id).forEach { tc ->
                        append(
                            """
                        ### Example for Use Case: ${uc.id}
                        **Conversation**
                        ${
                                tc.expectedConversation.joinToString("\n") {
                                    val prefix = if (it.role == "assistant") "<ID:${uc.id}>" else ""
                                    "${it.role}: $prefix ${it.content}"
                                }
                            }
                            
                            
                        """.trimIndent()
                        )
                    }
                }
            }

            // Add tools
            useCases.forEach { useCase ->
                useCase.extractTools().forEach {
                    addTool(it)
                }
            }

            // Add conditions
            val conditions = buildSet {
                isWeekend()?.let { add("is_weekend") }
                add(currentDate())
            }
            val useCasesPrompt = processUseCases(useCases = useCases, fallbackLimit = 3, conditions = conditions)

            // Output the final prompt
            val prompt = local("assistant.md")!!
                .replace("\$\$ROLE\$\$", role)
                .replace("\$\$EXAMPLES\$\$", examples)
                .replace("\$\$USE_CASES\$\$", useCasesPrompt)
                .replace("\$\$TIME\$\$", time())
            prompt
        }
    }
}.getAgents().first() as ConversationAgent
