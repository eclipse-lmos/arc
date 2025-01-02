// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.openai

data class OpenAINativeClientConfig(
    val modelName: String,
    val url: String,
    val apiKey: String,
) {

    override fun toString(): String {
        return "OpenAINativeClientConfig(modelName=$modelName, url=$url, apiKey=***)"
    }
}
