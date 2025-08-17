// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.llm

import org.eclipse.lmos.arc.agents.functions.LLMFunction

/**
 * Represents a tool call made by an LLM.
 *
 * @property tool The LLM function being called.
 * @property arguments The arguments passed to the function.
 * @property failed Indicates whether the tool call failed.
 */
data class LLMToolCall(
    val tool: LLMFunction,
    val arguments: Map<String, Any?>,
    val argumentsJson: String,
    val failed: Exception? = null,
)
