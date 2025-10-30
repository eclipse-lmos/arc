// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.spring

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import org.eclipse.lmos.arc.agents.AgentFinishedEvent
import org.eclipse.lmos.arc.agents.dsl.FilterExecutedEvent
import org.eclipse.lmos.arc.agents.dsl.extensions.RateLimitTimeoutEvent
import org.eclipse.lmos.arc.agents.dsl.extensions.RateLimitedEvent
import org.eclipse.lmos.arc.agents.events.Event
import org.eclipse.lmos.arc.agents.events.EventHandler
import org.eclipse.lmos.arc.agents.llm.LLMFinishedEvent
import org.eclipse.lmos.arc.agents.router.RouterRoutedEvent
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.getOrNull
import java.math.RoundingMode.DOWN
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * Converts events from the Arc Framework into performance metrics.
 */
class MetricsHandler(private val metrics: MeterRegistry) : EventHandler<Event> {

    override fun onEvent(event: Event) {
        when (event) {
            is AgentFinishedEvent -> with(event) {
                if (event.flowBreak) {
                    metrics.counter("arc.agent.flowBreak", "agent", agent.name).increment()
                }
                if (event.output is Success) {
                    timer(
                        "arc.agent.finished",
                        duration,
                        tags = mapOf(
                            "agent" to agent.name,
                            "flowBreak" to flowBreak.toString(),
                            "model" to (model ?: "default"),
                            "tools" to tools.joinToString(","),
                        ),
                    )
                } else {
                    timer(
                        "arc.agent.failed",
                        duration,
                        tags = mapOf("agent" to agent.name),
                    )
                }
            }

            is LLMFinishedEvent -> with(event) {
                val agentName = this.context?.get("agent") ?: ""
                val toolCallNames = result.getOrNull()?.toolCalls?.joinToString(",") { it.name }
                val content = result.getOrNull()?.content ?: ""
                val useCaseId = "<ID:(.*?)>".toRegex(RegexOption.IGNORE_CASE).find(content)?.groupValues?.get(1)
                timer(
                    "arc.llm.finished",
                    duration,
                    tags = buildMap {
                        put("model", model)
                        put("agent", agentName)
                        put("tools", functions?.joinToString(",") { it.name } ?: "")
                        toolCallNames?.takeIf { it.isNotEmpty() }?.let { put("called_tools", it) }
                        useCaseId?.takeIf { it.isNotEmpty() }?.let { put("useCaseId", it) }
                    },
                )
                metrics.counter("arc.llm.finished.executor", "model", model, "type", "totalTokens", "agent", agentName).increment(totalTokens.toDouble())
                metrics.counter("arc.llm.finished.executor", "model", model, "type", "promptTokens", "agent", agentName).increment(promptTokens.toDouble())
                metrics.counter("arc.llm.finished.executor", "model", model, "type", "completionTokens", "agent", agentName).increment(completionTokens.toDouble())
                if (functionCallCount > 0) {
                    metrics.counter("arc.llm.finished.executor", "model", model, "type", "function_calls", "agent", agentName).increment(functionCallCount.toDouble())
                }
            }

            is RateLimitedEvent -> with(event) {
                val agentName = this.context?.get("agent") ?: ""
                timer(
                    "arc.agent.rate.limited",
                    duration,
                    tags = mapOf("name" to name, "agent" to agentName),
                )
            }

            is RateLimitTimeoutEvent -> with(event) {
                val agentName = this.context?.get("agent") ?: ""
                timer(
                    "arc.agent.rate.timeout",
                    duration,
                    tags = mapOf("name" to name, "agent" to agentName),
                )
            }

            is FilterExecutedEvent -> with(event) {
                val agentName = this.context?.get("agent") ?: ""
                timer(
                    "arc.filter.executed",
                    duration,
                    tags = mapOf("name" to name, "agent" to agentName),
                )
            }

            is RouterRoutedEvent -> with(event) {
                val accuracy = destination?.accuracy?.toBigDecimal()?.setScale(1, DOWN)?.toString() ?: "-1"
                val destination = destination?.destination ?: "null"
                val agentName = this.context?.get("agent") ?: ""
                timer(
                    "arc.router.routed",
                    duration,
                    tags = mapOf("accuracy" to accuracy, "destination" to destination, "agent" to agentName),
                )
            }
        }
    }

    private fun timer(name: String, duration: Duration, tags: Map<String, String>) = Timer.builder(name)
        .tags(tags.map { (k, v) -> Tag.of(k, v) })
        .distributionStatisticExpiry(java.time.Duration.ofMinutes(5))
        .distributionStatisticBufferLength(50) // limit memory usage
        .publishPercentiles(0.5, 0.95)
        .percentilePrecision(2)
        .register(metrics)
        .record(duration.toJavaDuration())
}
