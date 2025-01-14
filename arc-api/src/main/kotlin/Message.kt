// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.api

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val role: String,
    val content: String,
    val format: String = "text",
    val turnId: String? = null,
)
