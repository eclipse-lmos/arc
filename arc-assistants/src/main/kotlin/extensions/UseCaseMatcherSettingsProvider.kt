// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.extensions

/**
 * Supplies runtime settings for [org.eclipse.lmos.arc.assistants.support.filters.UseCaseMatcher].
 *
 * Register an implementation as a Spring bean (or via [org.eclipse.lmos.arc.agents.dsl.BeanProvider])
 * to configure how many conversation messages are sent to the matcher LLM.
 */
interface UseCaseMatcherSettingsProvider {
    val maxMessages: Int
}
