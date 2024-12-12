// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.llm

import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.failWith
import org.eclipse.lmos.arc.core.result

/**
 * Creates text embeddings for a given text.
 */
interface TextEmbedder {
    suspend fun embed(texts: List<String>): Result<TextEmbeddings, ArcException>
}

suspend fun TextEmbedder.embed(text: String) = result<TextEmbedding, ArcException> {
    embed(listOf(text)).failWith { it }.embeddings.first()
}

/**
 * A text embedding.
 * @param text The text that was embedded.
 * @param embedding The embedding of the text.
 * @param labels Labels can be used to categorize the text, for example,
 * "sports" can be used to label text as something to do with sports.
 * @param data data can be used to store additional information that is not part of the embedding.
 * For example, if the text is a question, then data can contain the answer.
 */
@Serializable
data class TextEmbedding(
    val text: String,
    val embedding: List<Double>,
    val labels: Set<String> = emptySet(),
    val data: String? = null,
)

/**
 * A collection of text embeddings.
 */
@Serializable
class TextEmbeddings(val embeddings: List<TextEmbedding>) {

    fun findSimilar(text: TextEmbedding, similarity: Double): List<TextEmbedding> {
        return embeddings.filter { similarity(it.embedding, text.embedding) >= similarity }
    }

    fun findClosest(text: TextEmbedding): Pair<TextEmbedding, Double> {
        var currentSimilarity = -1.0
        var currentTextEmbedding = embeddings.first()
        embeddings.forEach {
            val s = similarity(text.embedding, it.embedding)
            if (s >= currentSimilarity) {
                currentSimilarity = s
                currentTextEmbedding = it
            }
        }
        return currentTextEmbedding to currentSimilarity
    }
}
