// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.assistants.support.usecases.validation

import org.eclipse.lmos.arc.assistants.support.usecases.UseCase

interface UseCaseValidator {

    fun validate(useCases: List<UseCase>): UseCaseValidationError?
}

sealed interface UseCaseValidationError {
    val message: String
    val code: String
    val useCases: List<String>
}
