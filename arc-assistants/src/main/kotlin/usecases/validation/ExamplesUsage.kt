// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.assistants.support.usecases.validation

import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.assistants.support.usecases.UseCase

/**
 * Validator to ensure all the same examples are not used in multiple use cases.
 */
object UniqueExamples : UseCaseValidator {

    override fun validate(useCases: List<UseCase>): UseCaseValidationError? {
        val examples = mutableMapOf<String, UseCase>()
        val duplicates = mutableSetOf<String>()
        val duplicateExamples = mutableSetOf<String>()
        useCases.forEach { uc ->
            uc.examples.lines().filter { it.isNotEmpty() }.forEach { example ->
                val trimmed = example.trim()
                val usedExampleUc = examples[trimmed]
                if (usedExampleUc != null && usedExampleUc.id != uc.id && uc conditionsMatch usedExampleUc) {
                    duplicates.add(usedExampleUc.id)
                    duplicates.add(uc.id)
                    duplicateExamples.add(trimmed)
                }
                examples[trimmed] = uc
            }
        }
        if (duplicates.isNotEmpty()) return ExamplesReUsed(useCases = duplicates.toList(), examples = duplicateExamples)
        return null
    }

    /**
     * Checks if the conditions of two use cases match, i.e. if it is possible
     * for both use cases to be valid at the same time.
     * If they can be valid at the same time, then reusing examples is not allowed.
     */
    private infix fun UseCase.conditionsMatch(other: UseCase): Boolean {
        if (other.conditions.isEmpty()) return true
        if (conditions.isEmpty()) return true
        if (conditions.all { condition -> other.conditions.contains(condition) }) return true
        if (other.conditions.all { condition -> conditions.contains(condition) }) return true
        return false
    }
}

@Serializable
data class ExamplesReUsed(
    override val code: String = "REUSED_EXAMPLES",
    override val useCases: List<String>,
    val examples: Set<String>,
    override val message: String = "The same examples were used in multiple Use Cases: [${useCases.joinToString(", ")}] Examples: [${examples.joinToString(", ")}]",
) : UseCaseValidationError
