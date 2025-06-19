// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases

import org.eclipse.lmos.arc.assistants.support.common.UseCaseConstants.USE_CASE_ID_REGEX

typealias UseCaseId = String

/**
 * Extract the use case id from the assistant message.
 * For example, "<ID:useCaseId>"
 */

fun extractUseCaseId(message: String): Pair<String, UseCaseId?> {
    val id = USE_CASE_ID_REGEX.find(message)?.groupValues?.elementAtOrNull(1)?.trim()
    val cleanedMessage = message.replace(USE_CASE_ID_REGEX, "").trim()
    return cleanedMessage to id
}

/**
 * Extract the use case step id from the assistant message.
 * For example, "<Step 1>"
 */
private val useCaseStepIdRegex = "<Step (\\w*)>".toRegex()

fun extractUseCaseStepId(message: String): Pair<String, UseCaseId?> {
    val id = useCaseStepIdRegex.find(message)?.groupValues?.elementAtOrNull(1)
    val cleanedMessage = message.replace(useCaseStepIdRegex, "").replace("<No Step>", "").trim()
    return cleanedMessage to id
}
