// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.extensions

import org.eclipse.lmos.arc.agents.dsl.DSLContext

/**
 * Builds the system prompt for [org.eclipse.lmos.arc.assistants.support.filters.UseCaseMatcher].
 *
 * Register an implementation as a Spring bean (or via [org.eclipse.lmos.arc.agents.dsl.BeanProvider])
 * to override the default classification prompt at runtime.
 */
fun interface UseCasePromptProvider {
    /**
     * @param useCases Formatted use case definitions passed to the matcher LLM call.
     * @param context Current DSL context (e.g. for loading external prompt files).
     */
    suspend fun buildSystemPrompt(useCases: String, context: DSLContext): String
}
