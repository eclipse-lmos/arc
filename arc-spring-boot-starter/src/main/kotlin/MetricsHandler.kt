// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.spring

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import org.eclipse.lmos.arc.agents.AgentFinishedEvent
import org.eclipse.lmos.arc.agents.events.Event
import org.eclipse.lmos.arc.agents.events.EventHandler
import org.eclipse.lmos.arc.agents.llm.LLMFinishedEvent
import org.eclipse.lmos.arc.agents.router.RouterRoutedEvent
import org.eclipse.lmos.arc.core.Success
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
                timer(
                    "arc.llm.finished",
                    duration,
                    tags = mapOf("model" to model),
                )
            }

            is RouterRoutedEvent -> with(event) {
                val accuracy = destination?.accuracy?.toBigDecimal()?.setScale(1, DOWN)?.toString() ?: "-1"
                val destination = destination?.destination ?: "null"
                timer(
                    "arc.router.routed",
                    duration,
                    tags = mapOf("accuracy" to accuracy, "destination" to destination),
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
