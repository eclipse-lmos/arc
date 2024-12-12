// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.router

import org.eclipse.lmos.arc.agents.events.BaseEvent
import org.eclipse.lmos.arc.agents.events.Event
import java.time.Instant
import kotlin.time.Duration

/**
 * Events published by the LLM clients.
 */

data class RouterReadyEvent(
    override val timestamp: Instant = Instant.now(),
) : Event by BaseEvent()

data class RouterRoutedEvent(
    val request: String,
    val destination: Destination?,
    val duration: Duration,
    override val timestamp: Instant = Instant.now(),
) : Event by BaseEvent()
