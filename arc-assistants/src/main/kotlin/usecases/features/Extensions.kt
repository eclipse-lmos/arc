// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.assistants.support.usecases.features

import org.eclipse.lmos.arc.assistants.support.usecases.UseCase

typealias Formatter = suspend (String, UseCase, List<UseCase>?, List<String>) -> String

operator fun Formatter.plus(
    other: suspend (String, UseCase, List<UseCase>?, List<String>) -> String
): suspend (String, UseCase, List<UseCase>?, List<String>) -> String = { s, uc, all, conditions ->
    val firstResult = this(s, uc, all, conditions)
    other(firstResult, uc, all, conditions)
}