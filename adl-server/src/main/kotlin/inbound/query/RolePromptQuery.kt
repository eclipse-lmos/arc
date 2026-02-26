// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.inbound.query

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import org.eclipse.lmos.adl.server.models.RolePrompt
import org.eclipse.lmos.adl.server.repositories.RolePromptRepository

/**
 * GraphQL Query for retrieving role prompts.
 */
class RolePromptQuery(private val rolePromptRepository: RolePromptRepository) : Query {

    @GraphQLDescription("Retrieves all role prompts.")
    suspend fun rolePrompts(): List<RolePrompt> = rolePromptRepository.findAll()

    @GraphQLDescription("Retrieves a role prompt by ID.")
    suspend fun rolePrompt(
        @GraphQLDescription("The unique identifier of the role prompt") id: String
    ): RolePrompt? = rolePromptRepository.findById(id)
}

