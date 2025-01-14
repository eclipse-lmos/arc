// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
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
