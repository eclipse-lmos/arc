// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.graphql

import org.eclipse.lmos.arc.agents.AgentFailedException
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.api.AgentRequest
import org.eclipse.lmos.arc.core.Result

/**
 * Context handlers are a great way to dynamically inject context / beans on each request.
 */
interface ContextHandler {

    suspend fun inject(
        request: AgentRequest,
        block: suspend (Set<Any>) -> Result<Conversation, AgentFailedException>,
    ): Result<Conversation, AgentFailedException>
}

/**
 * Noop context handler.
 */
class EmptyContextHandler : ContextHandler {
    override suspend fun inject(
        request: AgentRequest,
        block: suspend (Set<Any>) -> Result<Conversation, AgentFailedException>,
    ): Result<Conversation, AgentFailedException> {
        return block(emptySet())
    }
}

/**
 * Combines two context handlers into a single context handler.
 */
class CombineContextHandler(
    private val contextHandler1: ContextHandler,
    private val contextHandler2: ContextHandler,
) : ContextHandler {
    override suspend fun inject(
        request: AgentRequest,
        block: suspend (Set<Any>) -> Result<Conversation, AgentFailedException>,
    ): Result<Conversation, AgentFailedException> {
        return contextHandler1.inject(request) { extraContext1 ->
            contextHandler2.inject(request) { extraContext2 ->
                block(extraContext1 + extraContext2)
            }
        }
    }
}

/**
 * Combine multiple context handlers into a single context handler.
 */
fun List<ContextHandler>.combine(): ContextHandler {
    if (isEmpty()) return EmptyContextHandler()
    var handler: ContextHandler = EmptyContextHandler()
    forEach {
        handler = CombineContextHandler(handler, it)
    }
    return handler
}
