// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases.code

import org.eclipse.lmos.arc.core.getOrThrow
import org.slf4j.LoggerFactory
import java.util.ServiceLoader

/**
 * Processes markdown strings and replaces code blocks with their execution results.
 *
 * This class scans for markdown code blocks (denoted by triple backticks) and attempts
 * to execute them using registered CodeBlockRunner implementations discovered via ServiceLoader.
 *
 * Features:
 * - Detects markdown code blocks with optional language identifiers
 * - Uses ServiceLoader to discover CodeBlockRunner implementations
 * - Replaces code blocks with execution results
 * - Preserves original text if no suitable runner is found
 *
 * Example input:
 * ```
 * Here is some code:
 * ```kotlin
 * 1 + 1
 * ```
 * ```
 *
 * Example output (if runner executes successfully):
 * ```
 * Here is some code:
 * 2
 * ```
 */
class CodeBlockProcessor {

    private val log = LoggerFactory.getLogger(CodeBlockProcessor::class.java)
    private val codeBlockRunners: List<CodeBlockRunner> by lazy {
        ServiceLoader.load(CodeBlockRunner::class.java).toList()
    }

    /**
     * Processes a string containing markdown code blocks and replaces them with execution results.
     *
     * Code blocks are identified by triple backticks (```). The method will:
     * 1. Parse the string to find all code blocks
     * 2. For each code block, find a CodeBlockRunner that can handle it
     * 3. Execute the code block using the runner
     * 4. Replace the code block with the execution result
     *
     * If no runner can handle a code block, it is left unchanged.
     *
     * @param input The markdown string to process
     * @return The processed string with code blocks replaced by execution results
     * @throws CodeException if execution of any code block fails
     */
    suspend fun process(input: String): String {
        log.debug("Processing string with {} code block runners available", codeBlockRunners.size)

        val codeBlocks = extractCodeBlocks(input)

        if (codeBlocks.isEmpty()) {
            log.debug("No code blocks found in input")
            return input
        }

        log.debug("Found {} code blocks to process", codeBlocks.size)

        var result = input
        codeBlocks.forEach { (original, codeBlock) ->
            val runner = codeBlockRunners.firstOrNull { it.canHandle(codeBlock) }

            if (runner != null) {
                log.debug(
                    "Executing code block with language '{}' using {}",
                    codeBlock.language,
                    runner::class.simpleName,
                )

                val executionResult = runner.run(codeBlock).getOrThrow() ?: ""
                result = result.replace(original, executionResult)
                log.debug("Replaced code block with execution result: $executionResult")
            } else {
                log.debug("No runner found for code block with language '{}'", codeBlock.language)
            }
        }

        return result
    }

    /**
     * Extracts all code blocks from the input string.
     *
     * @param input The string to extract code blocks from
     * @return A list of pairs containing the original markdown text and the extracted CodeBlock
     */
    private fun extractCodeBlocks(input: String): List<Pair<String, CodeBlock>> {
        val codeBlocks = mutableListOf<Pair<String, CodeBlock>>()
        val lines = input.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]

            // Check for start of code block
            if (line.trimStart().startsWith("```")) {
                val language = line.trimStart().removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                val startIndex = i
                i++

                // Collect code lines until closing backticks
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }

                if (i < lines.size) {
                    // Found closing backticks
                    val code = codeLines.joinToString("\n")
                    val originalBlock = buildString {
                        appendLine(lines[startIndex])
                        codeLines.forEach { appendLine(it) }
                        append(lines[i])
                    }

                    codeBlocks.add(originalBlock to CodeBlock(code = code, language = language))
                    log.trace("Extracted code block: language='{}', length={}", language, code.length)
                }
            }
            i++
        }

        return codeBlocks
    }

    /**
     * Checks if any runner can handle the given language.
     *
     * @param language The programming language identifier
     * @return true if at least one runner can handle this language
     */
    fun canHandleLanguage(language: String): Boolean {
        val testBlock = CodeBlock(code = "", language = language)
        return codeBlockRunners.any { it.canHandle(testBlock) }
    }
}
