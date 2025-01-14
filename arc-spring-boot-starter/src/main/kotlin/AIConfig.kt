// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Simple setup for loading configuration properties.
 */
@ConfigurationProperties(prefix = "arc.ai")
data class AIConfig(val clients: List<AIClientConfig>)

data class AIClientConfig(
    val id: String,
    val client: String,
    val modelName: String,
    val url: String? = null,
    val apiKey: String? = null,
    val accessKey: String? = null,
    val accessSecret: String? = null,
)
