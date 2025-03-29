// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.functions

import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.Success

/**
 * A function decorator that listens for the result of the function
 * and calls a listener function with the result.
 */
class ListenableFunction(
    private val fn: LLMFunction,
    private val listener: suspend (String) -> Unit,
) :
    LLMFunction by fn {

    override suspend fun execute(input: Map<String, Any?>): Result<String, LLMFunctionException> {
        return fn.execute(input).also { if (it is Success) listener(it.value) }
    }
}
