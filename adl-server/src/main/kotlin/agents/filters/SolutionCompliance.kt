// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.agents.filters

import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.dsl.AgentOutputFilter
import org.eclipse.lmos.arc.agents.dsl.OutputFilterContext
import org.eclipse.lmos.arc.agents.dsl.extensions.getCurrentUseCases
import org.slf4j.LoggerFactory
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.store.embedding.CosineSimilarity
import org.eclipse.lmos.adl.server.repositories.StatisticsRepository
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.SystemMessage
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.extensions.outputContext
import org.eclipse.lmos.arc.agents.dsl.get
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.retry
import org.eclipse.lmos.arc.assistants.support.usecases.toUseCases
import org.eclipse.lmos.arc.core.getOrNull
import org.eclipse.lmos.arc.core.onFailure
import org.eclipse.lmos.arc.core.result
import kotlin.math.roundToInt

/**
 * Filter that checks if the output matches the solution of the current use case.
 */
class SolutionCompliance(
    private val embeddingModel: EmbeddingModel,
    private val threshold: Int = 72,
    private val statisticsRepository: StatisticsRepository? = null
) : AgentOutputFilter {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val solutionNotApplicable = "SOLUTION_NOT_APPLICABLE"
    private val outputDivider = "Final Answer >>"

    private fun system(useCase: String) =
        """
    ## Goal
    You are an AI assistant designed to generate responses that are compliant with the solution of the current use case. 
    Your task is to generate responses that are as close as possible to the solution in terms of content and meaning.
    
    ## Instructions 
    - Do not introduce new external information or make assumptions.
    - If the instructions require asking a question that has already been answered in the conversation history, return "$solutionNotApplicable".
        
      Use the following format in your response:
    
      Question: the input question you must answer.
      Thought: you should always think about what to do.
      Action: the action to take, examine the solution of the use case: 
      Does the solution require asking a question that has already been answered in the conversation history? 
      If yes, return "$solutionNotApplicable". Generate the final answer based on the solution of the use case.
      Observation: the result of the action.
      (Note: this Thought/Action/Observation can repeat N times)
      Thought: I now know the final answer.
      $outputDivider the final answer to the original input question.
      
    ## Current Use Case
    $useCase

  """

    override suspend fun filter(message: ConversationMessage, context: OutputFilterContext): ConversationMessage {
        val currentUseCases = context.getCurrentUseCases() ?: return message
        val currentUseCaseId = currentUseCases.currentUseCaseId ?: return message
        val originalUseCase = currentUseCases.currentUseCase() ?: return message
        val processedUseCase = currentUseCases.processedUseCaseMap[currentUseCaseId] ?: return message
        val solution =
            processedUseCase.toUseCases().firstOrNull()?.solution?.joinToString("\n")?.trim() ?: return message

        // Solutions that contain tool calls are not checked for compliance
        // as they often contain information that is not present in the solution and therefore the score
        // would be low even if the solution is correct.
        if (originalUseCase.extractTools().any { solution.contains(it) }) {
            log.debug("Skipping solution compliance check as the solution contains tool calls. \nSolution: $solution")
            return message
        }

        val solutionEmbedding = embeddingModel.embed(solution).content()
        val messageEmbedding = embeddingModel.embed(message.content).content()
        val similarity = CosineSimilarity.between(solutionEmbedding, messageEmbedding)
        val score = (similarity * 100).roundToInt()
        var output: AssistantMessage? = null

        context.outputContext("compliance", score.toString())
        context.getCurrentUseCases()?.currentUseCaseId?.let { useCaseId ->
             statisticsRepository?.recordComplianceScore(useCaseId, score)
        }

        if (score < threshold) {
            log.warn("Output message compliance score $score is below threshold $threshold!. \nSolution: $solution\nOutput: ${message.content}")
            context.getCurrentUseCases()?.currentUseCaseId?.let { useCaseId ->
                val useCase = context.getCurrentUseCases()?.processedUseCaseMap[useCaseId]!!
                context.callLLM(useCase).getOrNull()?.let {
                    if (it.content.contains(solutionNotApplicable)) {
                        context.retry("$solutionNotApplicable encountered!", max = 3)
                    }
                    log.info("Generated compliant response: ${it.content}")
                    output = AssistantMessage(it.content.substringAfter(outputDivider))
                }
            } ?: context.retry("Output message compliance score is below threshold.", max = 3)
        }
        return output ?: message
    }

    suspend fun DSLContext.callLLM(useCase: String) = result<AssistantMessage, ArcException> {
        val conversation = get<Conversation>()
        val chatCompleterProvider = get<ChatCompleterProvider>()
        val chatCompleter = chatCompleterProvider.provideByModel(null)
        return chatCompleter.complete(
            buildList {
                add(SystemMessage(system(useCase)))
                conversation.transcript.forEach { add(it) }
            },
            null,
            settings = ChatCompletionSettings(temperature = 0.0, seed = 42),
        )
    }.onFailure { log.warn("Solution compliance check failed to call LLM!", it) }
}
