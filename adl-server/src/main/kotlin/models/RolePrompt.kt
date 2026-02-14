// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.models

/**
 * Represents a role prompt.
 */
data class RolePrompt(
    val id: String,
    val name: String,
    val description: String,
    val tags: List<String>,
    val role: String,
    val tone: String,
)

