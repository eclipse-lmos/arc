// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents

import org.eclipse.lmos.arc.core.Result

/**
 * The main Agent interface.
 */
interface Agent<I, O> {

    val name: String

    val description: String

    /**
     * Executes the agent with the given input and context.
     * The objects passed as the context can be accessed within the Agents DSL using DSLContext#context.
     */
    suspend fun execute(input: I, context: Set<Any> = emptySet()): Result<O, AgentFailedException>
}

/**
 * Exception thrown when an agent fails to execute.
 */
open class AgentFailedException(msg: String, cause: Exception? = null) : Exception(msg, cause)

/**
 * Exception thrown when an agent does not execute.
 * Usually thrown when the input has been completely filtered out or some other condition is not met.
 */
class AgentNotExecutedException(msg: String, cause: Exception? = null) : AgentFailedException(msg, cause)
