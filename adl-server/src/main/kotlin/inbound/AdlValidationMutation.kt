// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import org.eclipse.lmos.arc.assistants.support.usecases.toUseCases
import kotlin.text.Regex

/**
 * GraphQL mutation for validating ADL code.
 * Takes ADL code as input and returns validation results including syntax errors, used tools, and references.
 */
class AdlValidationMutation : Mutation {

    /**
     * Validates the given ADL code and returns validation results.
     * @param adl The ADL code to validate.
     * @return A ValidationResult object containing syntax errors, used tools, and references.
     */
    @GraphQLDescription("Validates the given ADL code and returns syntax errors, used tools, and references.")
    suspend fun validate(adl: String): ValidationResult {
        val syntaxErrors = mutableListOf<SyntaxError>()
        val usedTools = mutableSetOf<String>()
        val references = mutableSetOf<String>()

        // Always perform syntax validation
        validateSyntax(adl, syntaxErrors)

        try {
            // Attempt to parse the ADL content
            val useCases = adl.toUseCases()

            // Extract tools and references from successfully parsed use cases
            useCases.forEach { useCase ->
                usedTools.addAll(useCase.extractTools())
                references.addAll(useCase.extractReferences())
            }
        } catch (e: Exception) {
            // Capture parsing errors as syntax errors
            syntaxErrors.add(
                SyntaxError(
                    line = null,
                    message = "Parsing error: ${e.message ?: e.javaClass.simpleName}",
                ),
            )
            // Even if parsing fails, try to extract tools and references using regex
            usedTools.addAll(extractToolsFromText(adl))
            references.addAll(extractReferencesFromText(adl))
        }

        return ValidationResult(
            syntaxErrors = syntaxErrors,
            usedTools = usedTools.sorted(),
            references = references.sorted(),
        )
    }

    /**
     * Performs additional syntax validation checks on the ADL content.
     */
    private fun validateSyntax(adl: String, errors: MutableList<SyntaxError>) {
        val lines = adl.lines()

        // Check for unbalanced brackets, braces, and parentheses
        val pairs = mapOf('(' to ')', '{' to '}', '[' to ']')
        val stack = mutableListOf<Pair<Char, Int>>()

        lines.forEachIndexed { lineIndex, line ->
            line.forEachIndexed { charIndex, char ->
                when {
                    char in pairs.keys -> {
                        stack.add(char to (lineIndex + 1))
                    }
                    char in pairs.values -> {
                        if (stack.isEmpty()) {
                            errors.add(
                                SyntaxError(
                                    line = lineIndex + 1,
                                    message = "Unmatched closing '$char' at position ${charIndex + 1}",
                                ),
                            )
                        } else {
                            val (opening, _) = stack.removeLast()
                            val expected = pairs[opening]
                            if (char != expected) {
                                errors.add(
                                    SyntaxError(
                                        line = lineIndex + 1,
                                        message = "Mismatched '$opening' with '$char' at position ${charIndex + 1}",
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Check for unclosed brackets
        stack.forEach { (char, lineNum) ->
            errors.add(
                SyntaxError(
                    line = lineNum,
                    message = "Unclosed '$char'",
                ),
            )
        }

        // Check for unclosed quotes
        lines.forEachIndexed { lineIndex, line ->
            val singleQuotes = line.count { it == '\'' }
            val doubleQuotes = line.count { it == '"' }
            if (singleQuotes % 2 != 0) {
                errors.add(
                    SyntaxError(
                        line = lineIndex + 1,
                        message = "Unclosed single quote",
                    ),
                )
            }
            if (doubleQuotes % 2 != 0) {
                errors.add(
                    SyntaxError(
                        line = lineIndex + 1,
                        message = "Unclosed double quote",
                    ),
                )
            }
        }

        // Check for mixed tabs and spaces in indentation
        val hasTabs = lines.any { it.startsWith("\t") }
        val hasSpaces = lines.any { it.matches(Regex("^ {2,}.*")) }
        if (hasTabs && hasSpaces) {
            errors.add(
                SyntaxError(
                    line = null,
                    message = "Mixed tabs and spaces for indentation",
                ),
            )
        }
    }

    /**
     * Extracts tools from text using regex patterns (fallback when parsing fails).
     */
    private fun extractToolsFromText(text: String): Set<String> {
        val tools = mutableSetOf<String>()

        // Pattern for @function() calls
        val functionPattern = Regex("""(?<=\s|\$)@([0-9A-Za-z_\-]+?)\(""")
        functionPattern.findAll(text).forEach {
            tools.add(it.groupValues[1])
        }

        // Pattern for common tool keywords
        val toolKeywordPattern = Regex("""\b(?:tools|uses|use|tool)\b[:=]?\s*([A-Za-z0-9_.-]+)""", RegexOption.IGNORE_CASE)
        toolKeywordPattern.findAll(text).forEach {
            tools.add(it.groupValues[1])
        }

        return tools
    }

    /**
     * Extracts references from text using regex patterns (fallback when parsing fails).
     */
    private fun extractReferencesFromText(text: String): Set<String> {
        val refs = mutableSetOf<String>()

        // Pattern for use case references #usecase_id
        val useCaseRefPattern = Regex("""(?<=\W|^)#([0-9A-Za-z_/\-]+)(?=[ .,]|$)""")
        useCaseRefPattern.findAll(text).forEach {
            refs.add(it.groupValues[1])
        }

        // Pattern for URLs
        val urlPattern = Regex("""https?://[^\s'\"<>]+""")
        urlPattern.findAll(text).forEach {
            refs.add(it.value)
        }

        // Pattern for file paths
        val filePathPattern = Regex("""(?:[A-Za-z]:)?[\\/][\w\-./\\]+\.[a-zA-Z0-9]+""")
        filePathPattern.findAll(text).forEach {
            refs.add(it.value)
        }

        // Pattern for explicit ref: tokens
        val refKeywordPattern = Regex("""\bref(?:erence)?s?\b[:=]?\s*([A-Za-z0-9_./:-]+)""", RegexOption.IGNORE_CASE)
        refKeywordPattern.findAll(text).forEach {
            refs.add(it.groupValues[1])
        }

        return refs
    }
}

/**
 * Data class representing a syntax error in the ADL code.
 */
data class SyntaxError(
    @GraphQLDescription("Line number where the error occurred (null if not line-specific)")
    val line: Int?,
    @GraphQLDescription("Error message describing the syntax issue")
    val message: String,
)

/**
 * Data class representing the validation result for ADL code.
 */
data class ValidationResult(
    @GraphQLDescription("List of syntax errors found in the ADL code")
    val syntaxErrors: List<SyntaxError>,
    @GraphQLDescription("List of tools used in the ADL code (extracted from function calls)")
    val usedTools: List<String>,
    @GraphQLDescription("List of references to other use cases found in the ADL code")
    val references: List<String>,
)
