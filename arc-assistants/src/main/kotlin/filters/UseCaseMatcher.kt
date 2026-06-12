// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.filters

import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.extensions.llm
import org.eclipse.lmos.arc.agents.dsl.getOptional
import org.eclipse.lmos.arc.assistants.support.extensions.UseCasePromptProvider
import org.eclipse.lmos.arc.core.getOrNull
import org.eclipse.lmos.arc.core.onFailure
import org.slf4j.LoggerFactory

class UseCaseMatcher(private val model: String? = null) {

    private val log = LoggerFactory.getLogger(this.javaClass)

    suspend fun matchUseCase(
        message: String,
        useCases: String,
        context: DSLContext
    ): String? {
        val systemPrompt = context.getOptional<UseCasePromptProvider>()
            ?.buildSystemPrompt(useCases, context)
            ?: UseCaseMatcherPrompts.build(useCases)
        val result = context.llm(
            system = systemPrompt,
            user = "Assistant Message: $message",
            model = model
        ).onFailure {
            log.warn("Failure while matching UseCase: $it")
        }.getOrNull() ?: return null
        log.debug("Matched UseCase: $result")
        val useCase = result.content.substringAfter("<", missingDelimiterValue = "")
            .substringBefore(">", missingDelimiterValue = "")
        if (useCase.isBlank()) return null
        if (!useCases.contains(useCase)) return null
        log.info("Matched UseCase found: $result")
        return useCase
    }
}
