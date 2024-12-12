// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.gemini

data class GeminiClientConfig(
    val modelName: String,
    val url: String,
)
