// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.azure

/**
 * GPT-5 and o-series models reject `max_tokens` and require `max_completion_tokens`.
 */
internal fun requiresMaxCompletionTokens(modelName: String?): Boolean {
    val model = modelName?.lowercase()?.replace('_', '-') ?: return false
    return "gpt-5" in model || O_SERIES_MODEL.containsMatchIn(model)
}

private val O_SERIES_MODEL = Regex("""(?<![a-z0-9])o[1-4](?![a-z0-9])""")
