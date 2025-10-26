// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.assistants.support.usecases.validation

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.eclipse.lmos.arc.assistants.support.usecases.UseCase
import org.eclipse.lmos.arc.assistants.support.usecases.toUseCases

/**
 * Validates and converts use cases from a JSON string to a UseCaseResult object.
 * Applies a series of validators to ensure the integrity of the use cases.
 */
class UseCaseToJson {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val validators = listOf(
        UniqueUseCaseId,
        UniqueExamples,
    )

    fun parse(useCases: String): UseCaseResult {
        val parsed = try {
            useCases.toUseCases()
        } catch (e: Exception) {
            return UseCaseResult(
                useCases = emptyList(),
                errors = listOf(
                    UseCaseSyntaxError(message = e.message ?: ""),
                ),
            )
        }
        val errors: List<UseCaseValidationError> = validators.mapNotNull { it.validate(parsed) }.toList()
        return UseCaseResult(useCases = parsed, errors = errors)
    }

    fun convert(useCases: String): String {
        return json.encodeToString(parse(useCases))
    }
}

@Serializable
data class UseCaseData(
    val useCases: String,
)

@Serializable
data class UseCaseResult(
    val useCases: List<UseCase>,
    val useCaseCount: Int = useCases.size,
    val errors: List<UseCaseValidationError>? = null,
    val recommendations: List<UseCaseRecommendation>? = null,
)

@Serializable
data class UseCaseRecommendation(
    val code: String,
    val message: String?,
)
