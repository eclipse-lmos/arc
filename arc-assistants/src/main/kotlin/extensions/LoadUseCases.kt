// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.assistants.support.usecases.formatToString
import org.eclipse.lmos.arc.assistants.support.usecases.toUseCases
import org.slf4j.LoggerFactory
import kotlin.random.Random.Default.nextInt

private val log = LoggerFactory.getLogger("UseCasesLoader")

/**
 * Loads the use case file with the given name.
 */
suspend fun DSLContext.useCases(name: String, fallbackLimit: Int = 2, conditions: Set<String> = emptySet()): String {
    val requestUseCase = system("usecase", defaultValue = "").takeIf { it.isNotEmpty() }
    val useCases =
        (requestUseCase ?: local(name))?.toUseCases() ?: kotlin.error("No use case file found with the name $name!")

    val usedUseCases = memory("usedUseCases") as List<String>? ?: emptyList()
    val fallbackCases = usedUseCases.groupingBy { it }.eachCount().filter { it.value >= fallbackLimit }.keys
    val filteredUseCases = useCases.formatToString(usedUseCases.toSet(), fallbackCases, loadConditions() + conditions)
    log.info("Loaded use cases: ${useCases.map { it.id }} Fallback cases: $fallbackCases")
    return filteredUseCases
}

/**
 * Loads default conditions.
 */
private fun loadConditions(): Set<String> = buildSet {
    // Add a condition with a 1 in 4 chance.
    // Helps to add some variety to the behaviour of the Agent.
    nextInt(1, 4).let { if (it == 2) add("sometimes") }
}
