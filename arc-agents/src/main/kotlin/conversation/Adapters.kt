// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.conversation

import org.eclipse.lmos.arc.agents.llm.LLMToolCall

/**
 * Converts an LLMToolCall to a ToolCall.
 *
 * @receiver LLMToolCall to be converted.
 * @return ToolCall representation of the LLMToolCall.
 */
fun LLMToolCall.toToolCall(): ToolCall {
    return ToolCall(name = tool.name, arguments = argumentsJson, failed = failed?.message)
}
