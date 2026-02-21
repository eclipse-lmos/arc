// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.agents

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.store.embedding.CosineSimilarity
import org.eclipse.lmos.adl.server.agents.extensions.ConversationGuider
import org.eclipse.lmos.adl.server.agents.extensions.InputHintProvider
import org.eclipse.lmos.adl.server.agents.extensions.currentDate
import org.eclipse.lmos.adl.server.agents.extensions.isWeekend
import org.eclipse.lmos.adl.server.repositories.AdlRepository
import org.eclipse.lmos.adl.server.repositories.TestCaseRepository
import org.eclipse.lmos.adl.server.repositories.UseCaseEmbeddingsRepository
import org.eclipse.lmos.adl.server.repositories.WidgetRepository
import org.eclipse.lmos.adl.server.services.McpService
import org.eclipse.lmos.adl.server.services.ClientEventPublisher
import org.eclipse.lmos.adl.server.repositories.StatisticsRepository
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
import org.eclipse.lmos.arc.assistants.support.usecases.features.processFlow
import org.eclipse.lmos.arc.assistants.support.usecases.toUseCases
import org.eclipse.lmos.adl.server.agents.extensions.UseCaseIdValidator
import org.eclipse.lmos.adl.server.agents.filters.ConvertToWidget
import org.eclipse.lmos.adl.server.agents.filters.SolutionCompliance
import org.eclipse.lmos.adl.server.repositories.RolePromptRepository
import org.eclipse.lmos.arc.agents.dsl.extensions.system


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
fun createAssistantAgent(
    mcpService: McpService,
    testRepository: TestCaseRepository,
    embeddingsRepository: UseCaseEmbeddingsRepository,
    adlRepository: AdlRepository,
    embeddingModel: EmbeddingModel,
    widgetRepository: WidgetRepository,
    rolePromptRepository: RolePromptRepository,
    clientEventPublisher: ClientEventPublisher,
    chatCompleterProvider: ChatCompleterProvider? = null
): ConversationAgent = agents(
    handlers = listOf(LoggingEventHandler(), clientEventPublisher),
    functionLoaders = listOf(mcpService),
    chatCompleterProvider = chatCompleterProvider
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
            +UseCaseIdValidator()
            +UseCaseResponseHandler()
            +ConversationGuider()
            getCurrentUseCases()?.processedUseCaseMap?.get(getCurrentUseCases()?.currentUseCaseId)?.let { uc ->
                val solution = uc.toUseCases().first().solution.joinToString("\n").trim()
                if ((solution.startsWith("\"") || solution.startsWith("- \"")) && solution.endsWith("\"")) {
                    outputMessage = outputMessage.update(solution.substringAfter("\"").substringBeforeLast("\""))
                }
            }
            +SolutionCompliance(embeddingModel)
            +ConvertToWidget(widgetRepository)
            +UnresolvedDetector { "UNRESOLVED" }
        }
        prompt {
            val roleId = system("role", "default")
            val role = rolePromptRepository.findById(roleId)?.let {
                """
                    ## Role and Responsibilities
                    ${it.role}
                    
                    ## Language & Tone Requirements
                    ${it.tone}
                """.trimIndent()
            } ?: local("role.md")!!

            // Load Use Cases
            val currentUseCases = get<List<UseCase>>()
            val message = get<Conversation>().latest<UserMessage>()?.content
            val otherUseCases = embeddingsRepository.search(message!!, limit = 40, scoreThreshold = 0.7f)
                .distinctBy { it.adlId }
                .flatMap { adlRepository.getAsUseCases(it.adlId) }
            info("Loaded ${otherUseCases.size} additional use cases from embeddings store.")
            val baseUseCases = local("base_use_cases.md")?.toUseCases() ?: emptyList()

            // Convert steps to conditionals in use cases
            val useCases = (
                    baseUseCases.filter {
                        currentUseCases.none { bc -> bc.id == it.id }
                        otherUseCases.none { bc -> bc.id == it.id }
                    } + otherUseCases.filter {
                        currentUseCases.none { bc -> bc.id == it.id }
                    } + currentUseCases
                    ).map { uc ->
                    if (uc.steps.isNotEmpty()) {
                        val convertedSteps = uc.steps.filter { it.text.isNotEmpty() }.mapIndexed { i, step ->
                            step.copy(conditions = step.conditions + "step_${i + 1}")
                        }
                        uc.copy(solution = convertedSteps + uc.solution.map { s ->
                            s.copy(conditions = s.conditions + "else")
                        }, steps = emptyList())
                    } else uc
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
            val userMessageEmbedding = embeddingModel.embed(message)
            val useCasesPrompt =
                processUseCases(
                    useCases = useCases, fallbackLimit = 3, conditions = conditions, exampleLimit = 5,
                    formatter = { content, useCase, useCases, usedUseCases ->

                        var addExamples = false
                        val examples = buildString {
                            append("## Example Conversations:\n")
                            testRepository.findByUseCaseId(useCase.id).filter { it.contract }.forEach { tc ->
                                addExamples = true
                                append(
                                    """
                        Example Conversation: ${tc.name}
                        ${
                                        tc.expectedConversation.joinToString("\n") {
                                            val prefix = if (it.role == "assistant") "<ID:${useCase.id}>" else ""
                                            "${it.role}: $prefix ${it.content}"
                                        }
                                    }
                          
                        """.trimIndent()
                                )
                            }
                            append("----\n")
                        }

                        processFlow(
                            content = content,
                            useCase = useCase,
                            allUseCases = useCases,
                            usedUseCases = usedUseCases,
                            conditions = conditions,
                            context = this,
                            optionsAnalyser = { userMessage, options ->

                                // Try exact match first
                                options.options.firstOrNull {
                                    it.option.equals(userMessage.trim(), ignoreCase = true)
                                }?.let { return@processFlow it }

                                // Try cosine similarity match
                                // Try cosine similarity match to find the highest match
                                options.options.map {
                                    it to CosineSimilarity.between(
                                        userMessageEmbedding.content(),
                                        embeddingModel.embed(it.option).content()
                                    )
                                }.filter { it.second > 0.8 }
                                    .maxByOrNull { it.second }
                                    ?.first
                                    ?.let { return@processFlow it }
                                null
                            }
                        ).let {
                            if (addExamples) {
                                it.replace("----", "") + "\n\n$examples"
                            } else it
                        }
                    },
                )

            // Output the final prompt
            val prompt = local("assistant.md")!!
                .replace("\$\$ROLE\$\$", role)
                .replace("\$\$USE_CASES\$\$", useCasesPrompt)
                .replace("\$\$TIME\$\$", time())
            prompt
        }
    }
}.getAgents().first() as ConversationAgent
