// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.llm

data class ChatCompletionSettings(
    val temperature: Double? = null,
    val maxTokens: Int? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val n: Int? = null,
    val seed: Long? = null,
    val format: OutputFormat? = null,
)

enum class OutputFormat {
    TEXT,
    JSON,
}
