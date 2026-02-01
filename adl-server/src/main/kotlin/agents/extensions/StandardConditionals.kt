// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.agents.extensions

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun isWeekend(): String? {
    val day = LocalDate.now().dayOfWeek
    return if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
        "is_weekend"
    } else {
        null
    }
}

fun currentDate(): String {
    return LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
}
