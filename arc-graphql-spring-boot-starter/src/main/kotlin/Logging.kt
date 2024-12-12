// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.graphql

import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.eclipse.lmos.arc.api.AgentRequest
import org.slf4j.MDC

suspend fun <T> withLogContext(
    agent: String,
    request: AgentRequest,
    block: suspend kotlinx.coroutines.CoroutineScope.() -> T,
): T {
    val current = MDC.getCopyOfContextMap() ?: emptyMap()
    val extraContext = mapOf(
        "agent" to agent,
        "conversationId" to request.conversationContext.conversationId,
        "turnId" to (request.conversationContext.turnId ?: "-1"),
    ) + request.systemContext.associate { it.key to it.value }
    return withContext(MDCContext(current + extraContext), block)
}
