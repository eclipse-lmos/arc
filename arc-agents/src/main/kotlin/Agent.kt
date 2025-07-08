// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents

import org.eclipse.lmos.arc.agents.agent.Skill
import org.eclipse.lmos.arc.core.Result

/**
 * The main Agent interface.
 *
 * An agent is a component that can perform specific tasks or operations.
 * It has a name, description, and can optionally provide a list of skills it can perform.
 *
 * @param I The type of input the agent accepts.
 * @param O The type of output the agent produces.
 */
interface Agent<I, O> {

    /**
     * The unique name of the agent.
     */
    val name: String

    /**
     * The set of flags that must be enabled for the agent to execute.
     */
    val activateOnFeatures: Set<String>?
        get() = null

    /**
     * The version of the agent.
     */
    val version: String

    /**
     * A human-readable description of what the agent does.
     */
    val description: String

    /**
     * Returns a list of skills that this agent can perform.
     *
     * @return A list of skills or null if the agent doesn't have any skills.
     */
    suspend fun fetchSkills(): List<Skill>?

    /**
     * Executes the agent with the given input and context.
     * The objects passed as the context can be accessed within the Agents DSL using DSLContext#context.
     *
     * @param input The input data for the agent to process.
     * @param context Additional contextual objects that can be used during execution.
     * @return A Result containing either the successful output or an AgentFailedException.
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
