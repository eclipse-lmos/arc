// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.inbound.rest

import io.ktor.server.routing.Route
import org.eclipse.lmos.adl.server.services.ClientEventPublisher
import io.ktor.server.sse.*
import io.ktor.sse.*
import kotlinx.coroutines.flow.forEach
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

fun Route.clientEvents(publisher: ClientEventPublisher) {
    sse("/events") {
        heartbeat {
            period = 5.seconds
            event = ServerSentEvent("heartbeat")
        }
        publisher.events.collect { event ->
            send(event)
        }
    }
}

