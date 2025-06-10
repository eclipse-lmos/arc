// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents

import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.getOptional

/**
 * Indicates that calling the Azure client has failed.
 * In the case of Hallucinations exceptions, the cause field will contain a HallucinationDetectedException.
 */
open class ArcException(msg: String = "Unexpected error!", override val cause: Exception? = null) :
    Exception(msg, cause)

/**
 * Indicates that the AI has performed incorrectly or unexpectedly.
 */
class HallucinationDetectedException(msg: String) : ArcException(msg)

/**
 * Indicates that a requested feature is not supported by the Model.
 */
class FeatureNotSupportedException(msg: String) : ArcException(msg)

/**
 * Indicates that the Model endpoint is not reachable.
 */
class ServerException(msg: String) : ArcException(msg)

/**
 * Indicates that the Model endpoint could not be called due to an authentication error.
 */
class AuthenticationException(msg: String) : ArcException(msg)

/**
 * Indicates that the provided settings are invalid.
 */
class InvalidSettingsException(msg: String) : ArcException(msg)

/**
 * Indicates that the provided function could not be found.
 */
class FunctionNotFoundException(val functionName: String) : ArcException("Cannot find function called $functionName!")

/**
 * Indicates that a model name is missing.
 */
class MissingModelNameException : ArcException("Model name is missing!")

/**
 * Exceptions implementing this interface will not cause the Agent to fail.
 * Instead, the Agent will return the attached Conversation.
 */
interface WithConversationResult {
    val conversation: Conversation
}

/**
 * An exception that does not denote an actual error, but can be used to signal that the agent should be re-run.
 * The details field can be used to pass information to the new run of the agent and can be accessed by the
 * get<RetrySignal>() function.
 */
class RetrySignal(val reason: String? = null, val details: Map<String, String> = emptyMap(), val count: Int = 1) :
    ArcException("Retry") {
    override fun toString(): String = "Retry(reason=$reason, details=$details)"
}

suspend fun DSLContext.retry(reason: String? = null, details: Map<String, String> = emptyMap(), max: Int = 100) {
    getOptional<RetrySignal>()?.let { signal ->
        if (signal.count < max) {
            throw RetrySignal(
                reason = reason,
                details = signal.details + details,
                count = signal.count + 1,
            )
        }
    } ?: throw RetrySignal(reason, details)
}
