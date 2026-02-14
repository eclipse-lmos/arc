// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.services

import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.store.embedding.CosineSimilarity
import org.eclipse.lmos.adl.server.agents.EvalEvidence
import org.eclipse.lmos.adl.server.agents.EvalOutput
import org.eclipse.lmos.adl.server.models.SimpleMessage
import kotlin.math.roundToInt

class ConversationEvaluator(
    private val embeddingModel: EmbeddingModel,
) {
    fun evaluate(
        conversation: List<SimpleMessage>,
        expectedConversation: List<SimpleMessage>,
        failureThreshold: Double = 0.8,
    ): EvalOutput {
        val n = minOf(conversation.size, expectedConversation.size)
        var compared = 0

        var totalSimilarity = 0.0
        var lowestSimilarity = 1.0
        val reasons = mutableListOf<String>()
        val evidence = mutableListOf<EvalEvidence>()

        if (conversation.size != expectedConversation.size) {
            reasons.add("Conversation length mismatch. Expected ${expectedConversation.size}, got ${conversation.size}.")
        }

        for (i in 0 until n) {
            val actual = conversation[i]
            val expected = expectedConversation[i]

            if (actual.role != expected.role) {
                reasons.add("Message $i: Role mismatch. Expected ${expected.role}, got ${actual.role}.")
                // Using 0 similarity for this message as role mismatch is significant
                continue
            }

            if(expected.role == "user") {
                // User messages should always match exactly
                continue
            }

            compared++

            val actualEmb = embeddingModel.embed(actual.content).content()
            val expectedEmb = embeddingModel.embed(expected.content).content()
            val similarity = CosineSimilarity.between(actualEmb, expectedEmb)

            totalSimilarity += similarity
            lowestSimilarity = minOf(lowestSimilarity, similarity)

            if (similarity < failureThreshold) {
                reasons.add("Message $i: Content similarity is low (${(similarity * 100).roundToInt()}%).")
                evidence.add(
                    EvalEvidence(
                        quote = actual.content,
                        mapsTo = expected.content,
                    ),
                )
            }
        }

        // If one is empty
        // val finalScore = if (compared > 0) (totalSimilarity / compared) * 100 else 0.0
        val finalScore = lowestSimilarity * 100
        val verdict = if (finalScore >= 90) "pass" else if (finalScore >= 60) "partial" else "fail"

        return EvalOutput(
            verdict = verdict,
            score = finalScore.roundToInt(),
            reasons = reasons,
            missingRequirements = emptyList(),
            violations = emptyList(),
            evidence = evidence,
        )
    }
}