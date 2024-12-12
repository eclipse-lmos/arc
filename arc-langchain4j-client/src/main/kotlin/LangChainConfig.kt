// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.langchain4j

data class LangChainConfig(
    val modelName: String,
    val url: String?,
    val apiKey: String?,
    val accessKeyId: String?,
    val secretAccessKey: String?,
) {

    override fun toString(): String {
        return "LangChainConfig(modelName=$modelName, url=$url, apiKey=${apiKey?.let { "***" }}, accessKeyId=$accessKeyId, secretAccessKey=${secretAccessKey?.let { "***" }})"
    }
}
