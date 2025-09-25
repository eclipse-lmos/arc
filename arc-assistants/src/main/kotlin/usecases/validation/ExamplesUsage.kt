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
        val examples = mutableMapOf<String, String>()
        useCases.forEach { uc ->
            uc.examples.lines().filter { it.isNotEmpty() }.forEach { example ->
                val trimmed = example.trim()
                val usedExampleId = examples[trimmed]
                if (usedExampleId != null && usedExampleId != uc.id) {
                    return ExamplesReUsed(useCases = listOf(usedExampleId, uc.id))
                }
                examples[trimmed] = uc.id
            }
        }
        return null
    }
}

@Serializable
data class ExamplesReUsed(
    override val code: String = "REUSED_EXAMPLES",
    override val useCases: List<String>,
    override val message: String = "The same examples were used in multiple use cases: ${useCases.joinToString(", ")}",
) : UseCaseValidationError
