// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.azure

data class AzureClientConfig(
    val modelName: String,
    val url: String,
    val apiKey: String,
) {

    override fun toString(): String {
        return "AzureClientConfig(modelName=$modelName, url=$url, apiKey=***)"
    }
}
