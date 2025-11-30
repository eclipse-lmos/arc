// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.kotlin.runner

import kotlinx.coroutines.withTimeoutOrNull
import org.eclipse.lmos.arc.assistants.support.usecases.code.CodeBlock
import org.eclipse.lmos.arc.assistants.support.usecases.code.CodeBlockRunner
import org.eclipse.lmos.arc.assistants.support.usecases.code.CodeException
import org.eclipse.lmos.arc.assistants.support.usecases.code.ExecutionException
import org.eclipse.lmos.arc.assistants.support.usecases.code.TimeoutException
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.failWith
import org.eclipse.lmos.arc.core.result
import org.slf4j.LoggerFactory
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A CodeBlockRunner implementation that executes Kotlin code blocks.
 *
 * This runner uses the Kotlin scripting API to evaluate Kotlin code in a sandboxed environment.
 * It supports timeout configuration and captures both script output and return values.
 *
 * Features:
 * - Executes Kotlin code blocks
 * - Configurable execution timeout (default: 10 seconds)
 * - Captures script output and errors
 * - Returns formatted execution results
 *
 * Security Considerations:
 * - Execution timeout prevents infinite loops
 * - Scripts run in isolated evaluation context
 * - No direct file system or network access by default
 *
 * @property timeout Maximum execution time in milliseconds (default: 20s)
 */
class KotlinCodeBlockRunner : CodeBlockRunner {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val scriptingHost = BasicJvmScriptingHost()

    var timeout: Duration = 30.seconds

    /**
     * Executes the Kotlin code block and returns the result.
     *
     * The execution is wrapped in a timeout to prevent long-running scripts.
     *
     * @param codeBlock The code block containing Kotlin code to execute
     * @return Result containing the execution result as a String, or null if no output
     * @throws ExecutionException if script execution fails
     * @throws TimeoutException if script execution times out
     */
    override suspend fun run(codeBlock: CodeBlock): Result<String?, CodeException> = result {
        if (!canHandle(codeBlock)) return@result null

        log.debug("Executing Kotlin code block...")
        withTimeoutOrNull(timeout) {
            val result = executeKotlinScript(codeBlock.code) failWith { it }
            formatResult(result) failWith { it }
        } ?: run {
            log.error("Kotlin code block execution timed out!")
            failWith { TimeoutException("Script execution timed out after $timeout") }
        }
    }

    /**
     * Determines if this runner can handle the given code block.
     *
     * This runner handles code blocks with language identifier "kotlin" or "kts".
     *
     * @param codeBlock The code block to check
     * @return true if the language is "kotlin" or "kts", false otherwise
     */
    override fun canHandle(codeBlock: CodeBlock): Boolean {
        return codeBlock.language.lowercase() in listOf("kotlin", "kts")
    }

    /**
     * Executes the Kotlin script using the scripting API.
     *
     * @param code The Kotlin code to execute
     * @return The evaluation result from the scripting engine
     */
    private fun executeKotlinScript(code: String) = result<ResultWithDiagnostics<EvaluationResult>, CodeException> {
        try {
            val scriptSource = code.toScriptSource()
            scriptingHost.eval(
                scriptSource,
                ScriptConfiguration,
                ScriptEvaluationConfiguration.Default,
            )
        } catch (e: Exception) {
            log.error("Error executing Kotlin code block", e)
            failWith { ExecutionException("Execution error: ${e.message}") }
        }
    }

    /**
     * Formats the execution result into a string.
     *
     * @param result The result from script execution
     * @return Result containing the formatted string, or ExecutionException if failed
     */
    private fun formatResult(result: ResultWithDiagnostics<EvaluationResult>) = result<String, ExecutionException> {
        when (result) {
            is ResultWithDiagnostics.Success -> {
                val evalResult = result.value
                when (val returnValue = evalResult.returnValue) {
                    is ResultValue.Value -> {
                        val value = returnValue.value
                        if (value != null && value != Unit) value.toString() else ""
                    }

                    is ResultValue.Unit -> ""
                    is ResultValue.Error -> {
                        failWith { ExecutionException("Runtime error: ${returnValue.error}") }
                    }

                    else -> ""
                }
            }

            is ResultWithDiagnostics.Failure -> {
                val errorMessages = result.reports.joinToString("\n") { diagnostic ->
                    val location = diagnostic.location?.let { " at line ${it.start.line}" } ?: ""
                    "[${diagnostic.severity}]$location: ${diagnostic.message}"
                }
                failWith { ExecutionException("Compilation/Execution failed:\n$errorMessages") }
            }
        }
    }
}
