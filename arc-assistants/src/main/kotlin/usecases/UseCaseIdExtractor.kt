// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases

typealias UseCaseId = String

/**
 * Extract the use case id from the assistant message.
 * For example, "<ID:useCaseId> "<UseCase:useCaseId>"
 */
private val useCaseIdRegex = "<ID:(.*?)>".toRegex(RegexOption.IGNORE_CASE)
private val useCaseIdRegex_alt = "<use[_]?case:(.*?)>".toRegex(RegexOption.IGNORE_CASE)

fun extractUseCaseId(message: String): Pair<String, UseCaseId?> {
    val id = useCaseIdRegex.find(message)?.groupValues?.elementAtOrNull(1)?.trim()
        ?: useCaseIdRegex_alt.find(message)?.groupValues?.elementAtOrNull(1)?.trim()
    val cleanedMessage = message.replace(useCaseIdRegex, "").replace(useCaseIdRegex_alt, "").trim()
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
