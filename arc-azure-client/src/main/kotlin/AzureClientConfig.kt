// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.azure

data class AzureClientConfig(
    val modelName: String? = null,
    val url: String? = null,
    val apiKey: String? = null,
) {

    override fun toString(): String {
        return "AzureClientConfig(modelName=$modelName, url=$url, apiKey=***)"
    }
}
