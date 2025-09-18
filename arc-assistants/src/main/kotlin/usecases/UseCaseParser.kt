// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases

import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.assistants.support.usecases.Section.ALTERNATIVE_SOLUTION
import org.eclipse.lmos.arc.assistants.support.usecases.Section.DESCRIPTION
import org.eclipse.lmos.arc.assistants.support.usecases.Section.EXAMPLES
import org.eclipse.lmos.arc.assistants.support.usecases.Section.FALLBACK_SOLUTION
import org.eclipse.lmos.arc.assistants.support.usecases.Section.GOAL
import org.eclipse.lmos.arc.assistants.support.usecases.Section.NONE
import org.eclipse.lmos.arc.assistants.support.usecases.Section.SOLUTION
import org.eclipse.lmos.arc.assistants.support.usecases.Section.STEPS
import org.eclipse.lmos.arc.assistants.support.usecases.Section.SUB_START

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
                currentUseCase = UseCase(
                    id = lineWithoutConditions.substringAfter(":").trim(),
                    version = version,
                    conditions = conditions,
                    subUseCase = line.contains("# Case"),
                )
                currentSection = if (currentUseCase?.subUseCase == true) SUB_START else NONE
            } else {
                currentSection = when {
                    line.contains("# Goal") -> GOAL
                    line.contains("# Description") -> DESCRIPTION
                    line.contains("# Solution") -> SOLUTION
                    line.contains("# Alternative") -> ALTERNATIVE_SOLUTION
                    line.contains("# Fallback") -> FALLBACK_SOLUTION
                    line.contains("# Step") -> STEPS
                    line.contains("# Example") -> EXAMPLES
                    else -> error("Unknown UseCase section: $line")
                }
                return@forEachLine
            }
        } else if (line.startsWith("----")) {
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
) {
    fun matches(allConditions: Set<String>): Boolean = conditions.matches(allConditions)

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
fun Set<String>.matches(allConditions: Set<String>): Boolean {
    return isEmpty() || (
            positiveConditionals().all { allConditions.contains(it) } && negativeConditionals().none {
                allConditions.contains(
                    it,
                )
            }
            )
}

/**
 * Returns negative Conditionals, for example "!beta", without the "!" prefix, so "beta".
 */
private fun Set<String>.negativeConditionals() = filter { it.startsWith("!") }
    .map { it.removePrefix("!") }

private fun Set<String>.positiveConditionals() = filter { !it.startsWith("!") }
