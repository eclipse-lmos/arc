// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.repositories.impl

import org.eclipse.lmos.adl.server.models.RolePrompt
import org.eclipse.lmos.adl.server.repositories.RolePromptRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of the RolePromptRepository.
 */
class InMemoryRolePromptRepository(initialPrompts: List<RolePrompt> = emptyList()) : RolePromptRepository {
    private val prompts = ConcurrentHashMap<String, RolePrompt>()

    init {
        prompts["default"] = RolePrompt(
            id = "default",
            name = "Default Role",
            description = "",
            tags = listOf("default"),
            role = """
                You are a helpful, friendly, and professional customer support assistant.
                You must not deviate from this role or assume the role of a different agent.
                Your goal is to provide clear, accurate, and concise answers while making the customer feel understood and supported.
                """.trimIndent(),
            tone = """
                - Be friendly, natural, and professional.
                - Briefly acknowledge or confirm the customer’s request in a natural way.
                  Example: “I understand you’d like to manually pay your bills.”
                - Speak directly to the customer using “you.”
                - Suggest actions positively (e.g., “You can…” instead of “You must…”).
                - Avoid robotic phrasing or overly formal wording.
                - Do not add unnecessary information.
                - Do not make assumptions beyond the provided context.
            """.trimIndent()
        )
    }

    override suspend fun findAll(): List<RolePrompt> = prompts.values.toList()

    override suspend fun findById(id: String): RolePrompt? = prompts[id]

    override suspend fun save(rolePrompt: RolePrompt): RolePrompt {
        prompts[rolePrompt.id] = rolePrompt
        return rolePrompt
    }

    override suspend fun delete(id: String): Boolean = prompts.remove(id) != null
}

