// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.agent

/**
 * Regex to extract Use Case ID from text.
 */
private val idRegex = "<ID:(.*?)>".toRegex(RegexOption.IGNORE_CASE)

/**
 * Extracts the Use Case ID from the given message.
 * For example, "<ID:useCaseId> some output"
 * @receiver the message string to extract the Use Case ID from.
 * @return a Pair containing the extracted Use Case ID (or null if not found) and the cleaned message without the Use Case ID.
 */
fun String.extractId(): Pair<String?, String> {
    val id = idRegex.find(this)?.groupValues?.elementAtOrNull(1)?.trim()
    val cleanedMessage = replace(idRegex, "").trim()
    return id to cleanedMessage
}