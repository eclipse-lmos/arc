// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases

/**
 * Formats the given use cases to a string.
 *
 * The useAlternatives set is used to filter solutions.
 * Solutions with the headline "#### Alternative Solution" are filtered
 * unless the use case name is contained in the given set of alternatives.
 * In this case, the "primary" solution is filtered.
 */
fun List<UseCase>.formatToString(
    useAlternatives: Set<String> = emptySet(),
    useFallbacks: Set<String> = emptySet(),
    conditions: Set<String> = emptySet(),
) =
    buildString {
        this@formatToString.forEach { useCase ->
            val useAlternative = useAlternatives.contains(useCase.id) && useCase.alternativeSolution.isNotEmpty()
            val useFallback = useFallbacks.contains(useCase.id) && useCase.fallbackSolution.isNotEmpty()
            append(
                """
            |### UseCase: ${useCase.id}
            |#### Description
            |${useCase.description}
            |
                """.trimMargin(),
            )
            if (useCase.steps.isNotEmpty()) {
                append("#### Steps\n")
                useCase.steps.forEach {
                    if (it.matches(conditions)) append("${it.text}\n")
                }
            }
            if (!useAlternative && !useFallback) {
                append("#### Solution\n")
                useCase.solution.forEach {
                    if (it.matches(conditions)) append("${it.text}\n")
                }
            }
            if (useAlternative && !useFallback) {
                append("#### Solution\n")
                useCase.alternativeSolution.forEach {
                    if (it.matches(conditions)) append("${it.text}\n")
                }
            }
            if (useFallback) {
                append("#### Solution\n")
                useCase.fallbackSolution.forEach {
                    if (it.matches(conditions)) append("${it.text}\n")
                }
            }
            if (useCase.examples.isNotEmpty()) {
                append("#### Examples\n")
                append("${useCase.examples}\n")
            }
            append("\n----\n\n")
        }
    }.replace("\n\n\n", "\n\n")
