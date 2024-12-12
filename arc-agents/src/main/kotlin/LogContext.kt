// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents

import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.MDC

/**
 * Add values to the log context.
 */
suspend fun <T> withLogContext(
    map: Map<String, String>,
    block: suspend kotlinx.coroutines.CoroutineScope.() -> T,
): T {
    return withContext(MDCContext(logContext() + map), block)
}

/**
 * Returns the current log context.
 */
fun logContext() = MDC.getCopyOfContextMap() ?: emptyMap()
