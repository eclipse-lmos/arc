// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.functions

import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.events.BaseEvent
import org.eclipse.lmos.arc.agents.events.Event
import org.eclipse.lmos.arc.core.Result
import java.time.Instant
import kotlin.time.Duration

/**
 * Events published by the LLM clients when calling functions.
 */

data class LLMFunctionStartedEvent(
    val name: String,
    val param: Map<String, Any?>,
    override val timestamp: Instant = Instant.now(),
) : Event by BaseEvent()

data class LLMFunctionCalledEvent(
    val name: String,
    val param: Map<String, Any?>,
    val result: Result<String, ArcException>,
    val duration: Duration,
    override val timestamp: Instant = Instant.now(),
) : Event by BaseEvent()
