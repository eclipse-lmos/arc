// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.events

import org.slf4j.MDC
import java.time.Instant

/**
 * Interface for all event types.
 */
interface Event {
    val timestamp: Instant
    val context: Map<String, String>
}

/**
 * Base class for events.
 * Can be used in combination with the 'by' keyword to implement the [Event] interface.
 * Example, class MyEvent : Event by BaseEvent()
 */
class BaseEvent(
    override val timestamp: Instant = Instant.now(),
    override val context: Map<String, String> = MDC.getCopyOfContextMap() ?: emptyMap(),
) : Event
