// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases

import org.eclipse.lmos.arc.assistants.support.usecases.Section.*

/**
 * Parses the given string into a list of use cases.
 */
fun String.toUseCases(): List<UseCase> {
    val useCases = mutableListOf<UseCase>()
    var currentUseCase: UseCase? = null
    var currentSection = NONE

    forEachLine { line ->
        if (line.startsWith("#")) {
            if (line.contains("# UseCase")) {
                currentUseCase?.let { useCases.add(it) }
                currentUseCase = UseCase(id = line.substringAfter(":").trim())
                currentSection = NONE
            } else {
                currentSection = when {
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

            NONE -> currentUseCase
        }
    }
    currentUseCase?.let { useCases.add(it) }
    return useCases
}

/**
 * Extracts conditions from a given string.
 * Conditions are defined in the format
 * "This is my string <Condition1, Condition2>".
 */
fun String.parseConditions(): Pair<String, Set<String>> {
    val regex = Regex("<(.*?)>")
    val conditions = regex.find(this)?.groupValues?.get(1)
    return replace(regex, "").trim() to (conditions?.split(",")?.map { it.trim() }?.toSet() ?: emptySet())
}

fun String.asConditional(): Conditional {
    val (text, conditions) = parseConditions()
    return Conditional(text, conditions)
}

/**
 * Splits a given string into a list of lines.
 */
private inline fun String.forEachLine(crossinline fn: (String) -> Unit) {
    return split("\n").forEach { fn(it + "\n") }
}

enum class Section {
    NONE,
    DESCRIPTION,
    SOLUTION,
    ALTERNATIVE_SOLUTION,
    FALLBACK_SOLUTION,
    STEPS,
    EXAMPLES,
}

data class UseCase(
    val id: String,
    val description: String = "",
    val steps: List<Conditional> = emptyList(),
    val solution: List<Conditional> = emptyList(),
    val alternativeSolution: List<Conditional> = emptyList(),
    val fallbackSolution: List<Conditional> = emptyList(),
    val examples: String = "",
)

data class Conditional(
    val text: String = "",
    val conditions: Set<String> = emptySet(),
) {
    operator fun plus(other: String): Conditional {
        return copy(text = text + other)
    }

    fun matches(allConditions: Set<String>): Boolean {
        return conditions.isEmpty() || conditions.all { allConditions.contains(it) }
    }
}
