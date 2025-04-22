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
    val description: String,
    val tags: List<String>,
    val examples: List<String>? = null,
    val inputModes: List<String>? = null,
    val outputModes: List<String>? = null,
)
