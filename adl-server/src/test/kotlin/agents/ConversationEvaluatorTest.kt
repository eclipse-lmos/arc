// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.agents

import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.output.Response
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.adl.server.inbound.SimpleMessage
import org.eclipse.lmos.adl.server.services.ConversationEvaluator
import org.junit.jupiter.api.Test

class ConversationEvaluatorTest {

    class MockEmbeddingModel : EmbeddingModel {
        override fun embed(text: String): Response<Embedding> {
            val vector = when (text) {
                "match" -> floatArrayOf(1.0f, 0.0f)
                "similar" -> floatArrayOf(0.99f, 0.01f)
                "somewhat_similar" -> floatArrayOf(0.85f, 0.52678f)
                "diff" -> floatArrayOf(0.0f, 1.0f)
                else -> floatArrayOf(0.5f, 0.5f)
            }
            return Response.from(Embedding.from(vector))
        }

        override fun embed(textSegment: TextSegment): Response<Embedding> {
            return embed(textSegment.text())
        }

        override fun embedAll(textSegments: List<TextSegment>): Response<List<Embedding>> {
            return Response.from(textSegments.map { embed(it).content() })
        }
    }

    private val evaluator = ConversationEvaluator(MockEmbeddingModel())

    @Test
    fun `test exact match`() {
        val conversation = listOf(SimpleMessage("user", "match"))
        val expected = listOf(SimpleMessage("user", "match"))

        val result = evaluator.evaluate(conversation, expected)

        assertThat(result.score).isEqualTo(100)
        assertThat(result.verdict).isEqualTo("pass")
        assertThat(result.reasons).isEmpty()
    }

    @Test
    fun `test completely different content`() {
        val conversation = listOf(SimpleMessage("user", "diff"))
        val expected = listOf(SimpleMessage("user", "match"))

        val result = evaluator.evaluate(conversation, expected)

        // Dot product of (0,1) and (1,0) is 0.
        assertThat(result.score).isEqualTo(0)
        assertThat(result.verdict).isEqualTo("fail")
        assertThat(result.reasons).isNotEmpty()
    }

    @Test
    fun `test role mismatch`() {
        val conversation = listOf(SimpleMessage("assistant", "match"))
        val expected = listOf(SimpleMessage("user", "match"))

        val result = evaluator.evaluate(conversation, expected)

        // Counted as 0 score
        assertThat(result.score).isEqualTo(0)
        assertThat(result.reasons).anyMatch { it.contains("Role mismatch") }
    }

    @Test
    fun `test length mismatch`() {
        val conversation = listOf(SimpleMessage("user", "match"))
        val expected = listOf(SimpleMessage("user", "match"), SimpleMessage("assistant", "match"))

        val result = evaluator.evaluate(conversation, expected)

        assertThat(result.score).isEqualTo(100)
        assertThat(result.reasons).anyMatch { it.contains("Conversation length mismatch") }
    }

    @Test
    fun `test custom threshold`() {
        val conversation = listOf(SimpleMessage("user", "somewhat_similar"))
        val expected = listOf(SimpleMessage("user", "match"))

        // Default threshold is 0.8, similarity is 0.85 -> should NOT contain low similarity reason
        val resultDefault = evaluator.evaluate(conversation, expected)
        assertThat(resultDefault.reasons).noneMatch { it.contains("Content similarity is low") }

        // Custom threshold 0.9 -> similarity 0.85 is lower -> should contain low similarity reason
        val resultHigh = evaluator.evaluate(conversation, expected, failureThreshold = 0.9)
        assertThat(resultHigh.reasons).anyMatch { it.contains("Content similarity is low") }
    }
}
