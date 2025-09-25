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
        val idCounts = useCases.groupingBy { it.id }.eachCount()
        val duplicates = idCounts.filter { it.value > 1 }.keys.toList()
        return if (duplicates.isEmpty()) null else DuplicateUseCaseIds(useCases = duplicates)
    }
}

@Serializable
data class DuplicateUseCaseIds(
    override val code: String = "DUPLICATE_USE_CASE_IDS",
    override val useCases: List<String>,
    override val message: String = "Duplicate use case IDs found: ${useCases.joinToString(", ")}",
) : UseCaseValidationError
