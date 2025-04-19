// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.llm

data class AIClientConfig(
    val id: String? = null,
    val client: String? = null,
    val modelName: String? = null,
    val endpoint: String? = null,
    val apiKey: String? = null,
    val accessKey: String? = null,
    val accessSecret: String? = null,
) {
    override fun toString(): String {
        val apiKeyMasked = apiKey?.let { "****" } ?: "null"
        val accessKeyMasked = accessKey?.let { "****" } ?: "null"
        val accessSecretMasked = accessSecret?.let { "****" } ?: "null"
        return "AIClientConfig(id=$id, client=$client, modelName=$modelName, endpoint=$endpoint, apiKey=$apiKeyMasked, accessKey=$accessKeyMasked, accessSecret=$accessSecretMasked)"
    }
}
