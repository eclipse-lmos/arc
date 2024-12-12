// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases

/**
 * Extract te use case id from the assistant message.
 * For example, "<ID:useCaseId>"
 */
fun extractUseCaseId(message: String): Pair<String, String> {
    return message.replace("<ID:.*>".toRegex(), "").trim() to message.substringAfter("<ID:").substringBefore(">")
}
