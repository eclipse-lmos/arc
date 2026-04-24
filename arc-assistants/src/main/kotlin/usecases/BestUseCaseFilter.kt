// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases

import org.slf4j.LoggerFactory
import kotlin.text.RegexOption.IGNORE_CASE

private val log = LoggerFactory.getLogger("usecases.selectBestUseCasePerId")

/**
 * Selects the best use case for each id based on the matching condition score.
 */
fun List<UseCase>.selectBestUseCasePerId(conditions: Set<String>, input: String?): List<UseCase> {
    return this
        .groupBy { it.id }
        .values
        .map { sameIdUseCases ->
            if (sameIdUseCases.size == 1) return@map sameIdUseCases.first()

            val scoredUseCases = sameIdUseCases.map { useCase ->
                useCase to useCase.matchingConditionScore(conditions, input)
            }
            val maxScore = scoredUseCases.maxOf { it.second }
            val highestRanked = scoredUseCases.filter { it.second == maxScore }
            val selectedUseCase = highestRanked.first().first

            if (highestRanked.size > 1) {
                highestRanked.drop(1).forEach { (removedUseCase, _) ->
                    log.error(
                        "Removed duplicate use case '{}' after tie on matching conditions; kept first occurrence.",
                        removedUseCase.id,
                    )
                }
            }
            selectedUseCase
        }
}

private fun UseCase.matchingConditionScore(conditions: Set<String>, input: String?): Int {
    val matchedRegexConditions = conditionsMatchingInput(input)
    val availableConditions = conditions + matchedRegexConditions
    return this.conditions.count { it.isConditionSatisfiedBy(availableConditions) }
}

private fun UseCase.conditionsMatchingInput(input: String?): Set<String> {
    if (input == null) return emptySet()
    return conditions.filter { it.isRegexConditional() && it.regex().containsMatchIn(input) }.toSet()
}

private fun String.isConditionSatisfiedBy(availableConditions: Set<String>): Boolean = when {
    contains(" or ") -> split(" or ").any { it.trim().isConditionSatisfiedBy(availableConditions) }
    startsWith("!") -> !availableConditions.contains(removePrefix("!").trim())
    else -> availableConditions.contains(trim())
}

private fun String.isRegexConditional(): Boolean = startsWith("regex:")
private fun String.regex(): Regex = Regex(substringAfter("regex:"), IGNORE_CASE)
