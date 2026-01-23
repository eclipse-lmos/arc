// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases.code

import org.eclipse.lmos.arc.core.Result

/**
 * Interface for running code blocks found in use cases.
 *
 * Implementations of this interface can be discovered using Java's ServiceLoader mechanism.
 * When a use case is formatted and contains a code block, all registered CodeBlockRunner
 * implementations will be invoked to process the code.
 *
 * Example usage:
 * 1. Create an implementation of this interface
 * 2. Register it in META-INF/services/org.eclipse.lmos.arc.assistants.support.usecases.CodeBlockRunner
 * 3. The implementation will be automatically discovered and called during use case formatting
 */
interface CodeBlockRunner {

    /**
     * Executes or processes the code block.
     *
     * @param codeBlock The code block to run, containing code content and language identifier.
     * @return A result string that can be used to augment the formatted output, or null if no output is needed.
     */
    suspend fun run(codeBlock: CodeBlock): Result<String?, CodeException>

    /**
     * Determines if this runner can handle the given code block.
     *
     * @param codeBlock The code block to check.
     * @return true if this runner can handle the code block, false otherwise.
     */
    fun canHandle(codeBlock: CodeBlock): Boolean
}

data class CodeBlock(val code: String, val language: String)

sealed class CodeException(message: String) : Exception(message)
class ExecutionException(message: String) : CodeException(message)
class TimeoutException(message: String) : CodeException(message)
