// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound.mutation

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import org.eclipse.lmos.adl.server.sessions.Sessions
import org.eclipse.lmos.adl.server.templates.TemplateLoader
import org.eclipse.lmos.arc.assistants.support.usecases.UseCase
import org.eclipse.lmos.arc.assistants.support.usecases.formatToString
import org.eclipse.lmos.arc.assistants.support.usecases.toUseCases
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * GraphQL mutation for generating system prompts from ADL code.
 */
class SystemPromptMutation(
    private val sessions: Sessions,
    private val templateLoader: TemplateLoader,
) : Mutation {

    /**
     * Generates a complete system prompt based on the given ADL code.
     * @param adl The ADL code to compile.
     * @param conditionals Optional list of conditionals to filter use cases.
     * @param sessionId Optional session ID. If provided, the session turn will be incremented.
     * @return An object containing the compiled system prompt.
     */
    @GraphQLDescription("Generates a complete system prompt from ADL code, including role and use cases.")
    suspend fun systemPrompt(
        adl: String,
        conditionals: List<String> = emptyList(),
        sessionId: String? = null,
    ): SystemPromptResult {
        // Parse ADL to use cases
        val useCases: List<UseCase> = adl.toUseCases()

        // Format use cases to string with conditionals
        val compiledUseCases = useCases.formatToString(conditions = conditionals.toSet())

        // Get current time
        val currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        // Build the system prompt using template loader
        val systemPrompt = templateLoader.render(time = currentTime, useCases = compiledUseCases)

        // Handle session if sessionId is provided
        val session = sessionId?.let { id -> sessions.incrementTurn(id) }

        return SystemPromptResult(
            systemPrompt = systemPrompt,
            useCaseCount = useCases.size,
            turn = session?.turn,
        )
    }
}

/**
 * Data class for the SystemPrompt result.
 */
data class SystemPromptResult(
    val systemPrompt: String,
    val useCaseCount: Int,
    val turn: Int? = null,
)
