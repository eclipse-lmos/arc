// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.assistants.support.usecases.validation

import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.assistants.support.usecases.UseCase

/**
 * Validator to ensure all use case IDs are unique.
 * Returns a DuplicateUseCaseIds error if duplicates are found.
 */
object UniqueUseCaseId : UseCaseValidator {

    override fun validate(useCases: List<UseCase>): UseCaseValidationError? {
        val temp = mutableMapOf<String, UseCase>()
        val duplicates = mutableSetOf<String>()
        useCases.forEach { uc ->
            val usedUseCase = temp[uc.id]
            if (usedUseCase != null && uc conditionsMatch usedUseCase) {
                duplicates.add(uc.id)
            }
            temp[uc.id] = uc
        }
        if (duplicates.isNotEmpty()) return DuplicateUseCaseIds(useCases = duplicates.toList())
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
data class DuplicateUseCaseIds(
    override val code: String = "DUPLICATE_USE_CASE_IDS",
    override val useCases: List<String>,
    override val message: String = "Duplicate use case IDs found: ${useCases.joinToString(", ")}",
) : UseCaseValidationError
