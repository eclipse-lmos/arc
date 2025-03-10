// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.extensions

import org.eclipse.lmos.arc.assistants.support.usecases.UseCase

/**
 * Local variables stored by the Use Case extensions.
 */
data class LoadedUseCases(
    val name: String,
    val useCases: List<UseCase>,
    val usedUseCases: List<String>,
    val processedUseCases: String,
    val currentStep: String? = null,
    val currentUseCaseId: String? = null,
)
