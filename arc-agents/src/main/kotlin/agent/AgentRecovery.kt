// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.agent

import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.RetrySignal
import org.eclipse.lmos.arc.agents.WithConversationResult
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.core.getOrNull
import org.eclipse.lmos.arc.core.onFailure
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("RecoverAgentFailure")

/**
 * Attempts to recover from an agent failure by handling different types of exceptions.
 *
 * This function provides several recovery strategies based on the type of exception:
 * 1. If the exception (or its cause) is a [WithConversationResult], it extracts the conversation from it
 * 2. If the exception (or its cause) is a [RetrySignal], it retries the agent execution
 * 3. Otherwise, it calls the provided [onFail] function to attempt recovery
 *
 * @param error The exception that caused the agent to fail
 * @param dslContext The DSL context in which the agent was executing
 * @param input The original conversation input to the agent
 * @param context The set of contextual objects for the agent execution
 * @param onFail A function that can handle failures and optionally produce a recovery message
 * @return A pair containing the (possibly modified) conversation and a boolean indicating successful recovery,
 *         or null if recovery failed
 */
suspend fun ConversationAgent.recoverAgentFailure(
    error: Exception,
    dslContext: DSLContext,
    input: Conversation,
    context: Set<Any>,
    onFail: suspend DSLContext.(Exception) -> AssistantMessage?,
): Pair<Conversation, Boolean>? {
    log.info("Agent $name interrupted by ${error.cause?.message ?: error.message}.")

    val cause = error.cause
    return when {
        error is WithConversationResult -> error.conversation to true

        cause is WithConversationResult -> cause.conversation to true

        error is RetrySignal -> retry(error, input, context)

        cause is RetrySignal -> retry(cause, input, context)

        else -> {
            return try {
                dslContext.onFail(error)?.let { msg ->
                    log.info("""onFail has recovered error $error with "$msg".""")
                    (input + msg) to true
                }
            } catch (ex: Exception) {
                return when (ex) {
                    is WithConversationResult -> ex.conversation to true

                    is RetrySignal -> retry(ex, input, context)

                    else -> {
                        log.warn("Ignoring error thrown in onFail: $ex")
                        null
                    }
                }
            }
        }
    }
}

/**
 * Retries the agent execution with the given retry signal added to the context.
 *
 * This function is called when a [RetrySignal] is encountered during agent execution.
 * It attempts to re-execute the agent with the original input and the retry signal
 * added to the context.
 *
 * @param retrySignal The retry signal that triggered the retry
 * @param input The original conversation input to the agent
 * @param context The set of contextual objects for the agent execution
 * @return A pair containing the conversation and a boolean indicating successful execution,
 *         or null if the retry failed
 */
private suspend fun ConversationAgent.retry(
    retrySignal: RetrySignal,
    input: Conversation,
    context: Set<Any>,
): Pair<Conversation, Boolean>? {
    log.info("Retrying Agent $name with $retrySignal...")
    val cleanContext = context.filter { it !is RetrySignal }.toSet()
    return execute(input, cleanContext + retrySignal).onFailure {
        log.warn("Agent retry failed! Returning original exception!", it)
    }.getOrNull()?.let { it to true }
}
