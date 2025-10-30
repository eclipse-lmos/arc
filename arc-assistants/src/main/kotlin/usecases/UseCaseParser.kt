// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases

import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.assistants.support.usecases.Section.*
import kotlin.text.RegexOption.IGNORE_CASE

/**
 * Parses the given string into a list of use cases.
 */
fun String.toUseCases(): List<UseCase> {
    val useCases = mutableListOf<UseCase>()
    var currentUseCase: UseCase? = null
    var currentSection = NONE
    val version = extractVersion(this)

    forEachLine { line ->
        if (line.trimStart().startsWith("#")) {
            if (line.contains("# UseCase") || line.contains("# Case")) {
                currentUseCase?.let { useCases.add(it) }
                val (lineWithoutConditions, conditions) = line.parseConditions()
                val useCaseHeader = lineWithoutConditions.substringAfter(":").trim()
                val (id, executionLimit) = parseUseCaseHeader(useCaseHeader)
                currentUseCase = UseCase(
                    id = id,
                    executionLimit = executionLimit,
                    version = version,
                    conditions = conditions,
                    subUseCase = line.contains("# Case"),
                )
                currentSection = if (currentUseCase?.subUseCase == true) SUB_START else NONE
            } else if (line.contains("# Category:")) {
                val category = line.substringAfter(":").trim()
                if (category.isEmpty()) error("Missing category in: ${line.trim()}")
                currentUseCase = currentUseCase?.copy(category = category)
            } else {
                currentSection = when {
                    line.contains("# Goal") -> GOAL
                    line.contains("# Description") -> DESCRIPTION
                    line.contains("# Solution") -> SOLUTION
                    line.contains("# Alternative") -> ALTERNATIVE_SOLUTION
                    line.contains("# Fallback") -> FALLBACK_SOLUTION
                    line.contains("# Step") -> STEPS
                    line.contains("# Example") -> EXAMPLES
                    else -> error("Unknown UseCase section: ${line.trim()}")
                }
                return@forEachLine
            }
        } else if (line.trimStart().startsWith("----")) {
            currentSection = NONE
        }
        currentUseCase = when (currentSection) {
            SOLUTION -> currentUseCase?.copy(
                solution = (currentUseCase?.solution ?: emptyList()) + line.asConditional(),
            )

            GOAL -> currentUseCase?.copy(
                goal = (currentUseCase?.goal ?: emptyList()) + line.asConditional(),
            )

            STEPS -> currentUseCase?.copy(steps = (currentUseCase?.steps ?: emptyList()) + line.asConditional())
            EXAMPLES -> currentUseCase?.copy(examples = (currentUseCase?.examples ?: "") + line)
            DESCRIPTION -> currentUseCase?.copy(
                description = (currentUseCase?.description ?: "") + line,
            )

            FALLBACK_SOLUTION -> currentUseCase?.copy(
                fallbackSolution = (currentUseCase?.fallbackSolution ?: emptyList()) + line.asConditional(),
            )

            ALTERNATIVE_SOLUTION -> currentUseCase?.copy(
                alternativeSolution = (currentUseCase?.alternativeSolution ?: emptyList()) + line.asConditional(),
            )

            SUB_START -> {
                if (!line.contains("# Case")) {
                    currentUseCase?.copy(
                        solution = (currentUseCase?.solution ?: emptyList()) + line.asConditional(),
                    )
                } else {
                    currentUseCase
                }
            }

            NONE -> currentUseCase
        }
    }
    currentUseCase?.let { useCases.add(it) }
    return useCases
}

/**
 * Parses a use case header string to extract the use case ID and execution limit.
 *
 * The header string is expected to follow the format: `id(executionLimit)`, where:
 * - `id` is the identifier of the use case.
 * - `executionLimit` is an optional integer specifying the execution limit. If not provided, it defaults to 1.
 *
 * Example inputs and outputs:
 * - Input: "usecase1" -> Output: Pair("usecase1", 1)
 * - Input: "usecase2 (5)" -> Output: Pair("usecase2", 5)
 * - Input: "usecase3 ()" -> Output: Pair("usecase3", 1)
 *
 * @param header The use case header string to parse.
 * @return A Pair containing the use case ID as a String and the execution limit as an Int. default limit is null if not specified.
 */
fun parseUseCaseHeader(header: String): Pair<String, Int?> {
    val regex = Regex("""^\s*([^\(\s]+)\s*(?:\(\s*(\d*)\s*\))?\s*$""")
    val match = regex.matchEntire(header)
    val id = match?.groups?.get(1)?.value ?: header.trim()
    val executionLimit = match?.groups?.get(2)?.value?.takeIf { it.isNotBlank() }?.toIntOrNull()
    return id to executionLimit
}

/**
 * Extracts functions from a given string.
 * Functions are defined in the format
 * "Call @my_function()".
 */
private val functionsRegex = Regex("(?<=\\s|\$)@([0-9A-Za-z_\\-]+?)\\(\\)")

fun String.parseFunctions(): Pair<String, Set<String>> {
    val replacements = mutableMapOf<String, String>()
    val functions = buildSet {
        functionsRegex.findAll(this@parseFunctions).iterator().forEach {
            add(it.groupValues[1])
            replacements[it.groupValues[0]] = it.groupValues[1]
        }
    }
    var cleanedText = this
    replacements.forEach { (key, value) ->
        cleanedText = cleanedText.replace(key, value).trim()
    }
    return cleanedText to functions
}

/**
 * Extracts references to other use cases.
 * Referenced use cases are defined in the format
 * "Go to use case #other_usecase".
 */
private val useCasesRegex = Regex("(?<=\\W|$)#([0-9A-Za-z_/\\-]+)(?=[ .,]|$)")

fun String.parseUseCaseRefs(): Pair<String, Set<String>> {
    val replacements = mutableMapOf<String, String>()
    val references = buildSet {
        useCasesRegex.findAll(this@parseUseCaseRefs).iterator().forEach {
            add(it.groupValues[1])
            replacements[it.groupValues[0]] = "#" + it.groupValues[1].substringAfterLast("/")
        }
    }
    var cleanedText = this
    replacements.forEach { (key, value) ->
        cleanedText = cleanedText.replace(key, value).trim()
    }
    return cleanedText to references
}

/**
 * Extracts conditions from a given string.
 * Conditions are defined in the format
 * "This is my string <Condition1, Condition2>".
 */
fun String.parseConditions(): Pair<String, Set<String>> {
    val regex = Regex("<(.*?)>")
    val conditions = buildSet {
        regex.findAll(this@parseConditions).map { it.groupValues[1] }.flatMap { condition ->
            condition.split(",").map { it.trim() }.toSet()
        }.toSet().let { addAll(it) }
    }
    return replace(regex, "").trim() to conditions
}

fun String.asConditional(): Conditional {
    val (text, conditions) = parseConditions()
    val (textAfterFunctions, functions) = text.parseFunctions()
    val (finalText, useCaseRefs) = textAfterFunctions.parseUseCaseRefs()
    val endConditional = conditions.contains("/")
    val finalConditions = conditions.filter { it != "/" }.toSet()
    return Conditional(finalText, finalConditions, functions, useCaseRefs, endConditional)
}

/**
 * Splits a given string into a list of lines and filters out comments.
 */
private inline fun String.forEachLine(crossinline fn: (String) -> Unit) {
    return split("\n").filter { !it.trimStart().startsWith("//") && !it.trimStart().startsWith("<!--") }
        .forEach { fn(it + "\n") }
}

enum class Section {
    NONE,
    SUB_START,
    DESCRIPTION,
    GOAL,
    SOLUTION,
    ALTERNATIVE_SOLUTION,
    FALLBACK_SOLUTION,
    STEPS,
    EXAMPLES,
}

@Serializable
data class UseCase(
    val id: String,
    val executionLimit: Int? = null,
    val version: String? = null,
    val description: String = "",
    val steps: List<Conditional> = emptyList(),
    val solution: List<Conditional> = emptyList(),
    val alternativeSolution: List<Conditional> = emptyList(),
    val fallbackSolution: List<Conditional> = emptyList(),
    val examples: String = "",
    val conditions: Set<String> = emptySet(),
    val goal: List<Conditional> = emptyList(),
    val subUseCase: Boolean = false,
    val category: String? = null,
) {
    fun matches(allConditions: Set<String>, input: String? = null): Boolean = conditions.matches(allConditions, input)

    fun extractReferences(): Set<String> = buildSet {
        steps.forEach { addAll(it.useCaseRefs) }
        solution.forEach { addAll(it.useCaseRefs) }
        alternativeSolution.forEach { addAll(it.useCaseRefs) }
        fallbackSolution.forEach { addAll(it.useCaseRefs) }
    }

    fun extractTools(): Set<String> = buildSet {
        steps.forEach { addAll(it.functions) }
        solution.forEach { addAll(it.functions) }
        alternativeSolution.forEach { addAll(it.functions) }
        fallbackSolution.forEach { addAll(it.functions) }
    }
}

@Serializable
data class Conditional(
    val text: String = "",
    val conditions: Set<String> = emptySet(),
    val functions: Set<String> = emptySet(),
    val useCaseRefs: Set<String> = emptySet(),
    val endConditional: Boolean = false,
) {
    operator fun plus(other: String): Conditional {
        return copy(text = text + other)
    }

    fun matches(allConditions: Set<String>): Boolean = conditions.matches(allConditions)
}

/**
 * Matches conditionals.
 */
fun Set<String>.matches(conditions: Set<String>, input: String? = null): Boolean {
    val allConditions = if (input != null && isNotEmpty()) {
        this.mapNotNull { condition ->
            if (!condition.isRegexConditional()) return@mapNotNull null
            if (condition.regex().containsMatchIn(input)) condition else null
        }.toSet() + this
    } else {
        conditions
    }
    return isEmpty() || (
        positiveConditionals().all { allConditions.contains(it) } && negativeConditionals().none {
            allConditions.contains(it)
        }
        )
}

/**
 * Returns negative Conditionals, for example "!beta", without the "!" prefix, so "beta".
 */
private fun Set<String>.negativeConditionals() = filter { it.startsWith("!") }
    .map { it.removePrefix("!") }

private fun Set<String>.positiveConditionals() = filter { !it.startsWith("!") }

/**
 * Evaluates regex conditionals against the given input string.
 * Returns a set of conditions where the regex pattern matched.
 * Example,
 * if the UseCase has a conditional "<regex:.*urgent.*>"
 * and the input string is "This is an urgent request",
 * the returned set will contain "regex:.*urgent.*". which would inturn enable the conditional.
 */
fun UseCase.regexConditionals(input: String?): Set<String> {
    if (input == null) return emptySet()
    return buildSet {
        addAll(conditions)
        steps.forEach { addAll(it.conditions) }
        solution.forEach { addAll(it.conditions) }
        alternativeSolution.forEach { addAll(it.conditions) }
        fallbackSolution.forEach { addAll(it.conditions) }
        goal.forEach { addAll(it.conditions) }
    }.mapNotNull { conditional ->
        if (conditional.isRegexConditional()) {
            conditional to conditional.regex()
        } else {
            null
        }
    }.mapNotNull { (conditional, regex) ->
        if (regex.containsMatchIn(input)) conditional else null
    }.toSet()
}

private fun String.isRegexConditional(): Boolean = startsWith("regex:")
private fun String.regex(): Regex = Regex(substringAfter("regex:"), IGNORE_CASE)
