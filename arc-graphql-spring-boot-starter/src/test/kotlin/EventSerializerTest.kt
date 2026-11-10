// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.graphql

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.events.BaseEvent
import org.eclipse.lmos.arc.graphql.inbound.toJson
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant

class EventSerializerTest {
    @Test
    fun `event timestamps and context preserve the Jackson 2 wire format`() {
        val event = BaseEvent(
            timestamp = Instant.parse("2025-01-01T00:00:00.123456789Z"),
            context = mapOf("agent" to "test"),
        )
        val serialized = event.toJson()
        val json = jacksonObjectMapper().readTree(serialized)

        assertThat(serialized).contains("\"timestamp\":1735689600.123456789")
        assertThat(json["context"]["agent"].stringValue()).isEqualTo("test")
    }
}
