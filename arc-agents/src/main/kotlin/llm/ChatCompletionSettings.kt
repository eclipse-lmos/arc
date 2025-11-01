// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.llm

import kotlin.reflect.KClass

data class ChatCompletionSettings(
    val temperature: Double? = null,
    val maxTokens: Int? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val n: Int? = null,
    val seed: Long? = null,
    val format: OutputFormat? = null,
    val model: String? = null,
    val deploymentName: String? = null,
    val outputSchema: OutputSchema? = null,
) {
    fun deploymentNameOrModel(): String? {
        return deploymentName ?: model
    }
}

enum class OutputFormat {
    TEXT,
    JSON,
}

data class OutputSchema(
    val name: String,
    val description: String,
    val type: KClass<*>,
)

/**
 * Assigns the deployment or model name to the settings.
 * If the settings are null, a new instance is created with the provided deployment name.
 *
 * @param deploymentOrModelName The deployment or model Name to assign.
 * @return The updated settings.
 */
fun ChatCompletionSettings?.assignDeploymentNameOrModel(deploymentOrModelName: String?): ChatCompletionSettings? {
    if (this == null) return ChatCompletionSettings(deploymentName = deploymentOrModelName)
    if (this.deploymentName == null) return copy(deploymentName = deploymentOrModelName)
    return this
}
