// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.functions

import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.getOrNull
import org.eclipse.lmos.arc.core.onFailure

class TraceableLLMFunction(private val tracer: AgentTracer, private val function: LLMFunction) :
    LLMFunction by function {

    override suspend fun execute(input: Map<String, Any?>): Result<String, LLMFunctionException> {
        return tracer.withSpan(
            "tool $name",
            mapOf(
                "name" to name,
                "parameters" to parameters.toString(),
                "description" to description,
                "sensitive" to isSensitive.toString(),
            ),
        ) { tags, _ ->
            function.execute(input).also { result ->
                tags.tag("input", input.toString())
                result.getOrNull()?.let { tags.tag("result", it) }
                result.onFailure { tags.tag("error", it.message ?: "unknown error") }
            }
        }
    }
}
