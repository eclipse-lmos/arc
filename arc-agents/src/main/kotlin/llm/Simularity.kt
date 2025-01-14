// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.llm

import kotlin.math.sqrt

/**
 * Calculates the cosine similarity between two vectors.
 * The result will range from -1.0 to 1.0. With 1.0 meaning the vectors are practically identical.
 */
fun similarity(vec1: List<Double>, vec2: List<Double>): Double {
    require(vec1.size == vec2.size) { "Vector sizes must be equal!" }
    val dotProduct = vec1.zip(vec2) { a, b -> a * b }.sum()
    val magnitude1 = sqrt(vec1.sumOf { it * it })
    val magnitude2 = sqrt(vec2.sumOf { it * it })
    return if (magnitude1 == 0.0 || magnitude2 == 0.0) {
        0.0
    } else {
        dotProduct / (magnitude1 * magnitude2)
    }
}
