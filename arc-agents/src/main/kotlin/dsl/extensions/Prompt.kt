// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.Data
import org.eclipse.lmos.arc.agents.dsl.addData
import org.eclipse.lmos.arc.agents.dsl.get
import org.eclipse.lmos.arc.agents.dsl.getOptional
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.Success

/**
 * Retrieves a prompt from an external source.
 * If no PromptRetriever is available, it will try to load the prompt from the local system.
 */
suspend fun DSLContext.promptText(
    name: String,
    args: Map<String, Any?>,
): Result<String, PromptException> {
    val result = getOptional<PromptRetriever>()?.fetchPromptText(name, args)
        ?: local(name)?.let { Success(it) }
        ?: return Failure(NoPromptFoundException(name))
    if (result is Success) addData(Data(name, result.value))
    return result
}

/**
 * Interface for retrieving simple prompts.
 */
interface PromptRetriever {

    suspend fun fetchPromptText(name: String, args: Map<String, Any?>): Result<String, PromptException>
}

/**
 * Exceptions
 */
sealed class PromptException(msg: String, cause: Throwable? = null) : Exception(msg, cause)

class PromptServerException(val promptName: String, cause: Throwable) :
    PromptException("Could not retrieve prompt from server: $promptName!", cause)

class NoPromptFoundException(val promptName: String) : PromptException("No prompt found under name: $promptName!")
