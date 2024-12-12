// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Extensions to provide the LLM with the current time.
 */
fun time(zoneId: String? = null) = formatDateTime("HH:mm", zoneId)
fun date(zoneId: String? = null) = formatDateTime("dd.MM", zoneId)
fun year(zoneId: String? = null) = formatDateTime("yyyy", zoneId)

fun formatDateTime(pattern: String, zoneId: String? = null): String {
    val formatter = DateTimeFormatter.ofPattern(pattern)
    val time = zoneId?.let { ZonedDateTime.now(ZoneId.of(it)) } ?: ZonedDateTime.now()
    return formatter.format(time)
}
