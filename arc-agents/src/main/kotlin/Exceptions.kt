// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents

import org.eclipse.lmos.arc.agents.conversation.Conversation

/**
 * Indicates that calling the Azure client has failed.
 * In the case of Hallucinations exceptions, the cause field will contain a HallucinationDetectedException.
 */
open class ArcException(msg: String = "Unexpected error!", override val cause: Exception? = null) : Exception(msg, cause)

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
class FunctionNotFoundException(msg: String) : ArcException(msg)

/**
 * Exceptions implementing this interface will not cause the Agent to fail.
 * Instead, the Agent will return the attached Conversation.
 */
interface WithConversationResult {
    val conversation: Conversation
}
