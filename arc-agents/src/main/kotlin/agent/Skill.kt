// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.agent

/**
 * Represents a skill that an agent can perform.
 */
data class Skill(
    val id: String,
    val name: String,
    val description: String? = null,
    val tags: List<String>? = null,
    val examples: List<String>? = null,
    val inputModes: List<String>?,
    val outputModes: List<String>?,
)
