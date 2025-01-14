// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl

/**
 * Magic value for the tools field to include all tools that are available.
 */
object AllTools : List<String> by listOf("*") {
    val symbol get() = first()
}
