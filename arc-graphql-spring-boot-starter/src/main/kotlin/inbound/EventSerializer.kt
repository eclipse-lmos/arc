// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.graphql.inbound

import org.eclipse.lmos.arc.agents.events.Event
import tools.jackson.module.kotlin.KotlinFeature.UseJavaDurationConversion
import tools.jackson.module.kotlin.jacksonMapperBuilder

private val eventObjectMapper = jacksonMapperBuilder {
    enable(UseJavaDurationConversion)
}.configureForJackson2().build()

/**
 * Serialize an Event to JSON.
 */
fun Event.toJson(): String = eventObjectMapper.writeValueAsString(this)
