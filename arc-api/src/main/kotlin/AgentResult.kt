// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.api

import kotlinx.serialization.Serializable

/**
 * AgentResult
 */
@Serializable
data class AgentResult(
    val status: String? = null,
    val responseTime: Double = -1.0,
    val messages: List<Message>,
    val anonymizationEntities: List<AnonymizationEntity>? = null,
)
