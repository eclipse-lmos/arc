// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.agent

data class AIClientConfig(
    val id: String? = null,
    val client: String? = null,
    val modelName: String? = null,
    val endpoint: String? = null,
    val apiKey: String? = null,
    val accessKey: String? = null,
    val accessSecret: String? = null,
    val toolSupported: Boolean = false,
)
