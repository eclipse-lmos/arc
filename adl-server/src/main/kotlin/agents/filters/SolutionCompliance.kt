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
import org.eclipse.lmos.arc.agents.retry
import org.eclipse.lmos.arc.assistants.support.usecases.toUseCases
import kotlin.math.roundToInt

/**
 * Filter that checks if the output matches the solution of the current use case.
 */
class SolutionCompliance(private val embeddingModel: EmbeddingModel, private val threshold: Int = 60) :
    AgentOutputFilter {
    private val log = LoggerFactory.getLogger(this::class.java)

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

        if (score < threshold) {
            log.warn("Output message compliance score $score is below threshold $threshold!. \nSolution: $solution\nOutput: ${message.content}")
            context.retry("Output message compliance score is below threshold.", max = 3)
        }
        return message
    }
}
