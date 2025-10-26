// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.assistants.support.usecases.validation

import kotlinx.serialization.Serializable

@Serializable
data class UseCaseSyntaxError(
    override val code: String = "SYNTAX_ERROR",
    override val useCases: List<String> = emptyList(),
    override val message: String,
) : UseCaseValidationError
