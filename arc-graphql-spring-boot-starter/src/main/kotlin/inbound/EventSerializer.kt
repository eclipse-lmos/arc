// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.graphql.inbound

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature.UseJavaDurationConversion
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import org.eclipse.lmos.arc.agents.events.Event

private val eventObjectMapper = jacksonMapperBuilder {
    enable(UseJavaDurationConversion)
}.addModule(JavaTimeModule()).build()

/**
 * Serialize an Event to JSON.
 */
fun Event.toJson(): String = eventObjectMapper.writeValueAsString(this)
