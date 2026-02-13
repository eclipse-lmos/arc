// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.inbound.mutation

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import org.eclipse.lmos.adl.server.models.RolePrompt
import org.eclipse.lmos.adl.server.repositories.RolePromptRepository

/**
 * GraphQL Mutation for managing role prompts.
 */
class RolePromptMutation(private val rolePromptRepository: RolePromptRepository) : Mutation {

    @GraphQLDescription("Updates or creates a role prompt.")
    suspend fun updateRolePrompt(
        @GraphQLDescription("Unique identifier for the role prompt") id: String,
        @GraphQLDescription("Name of the role") name: String,
        @GraphQLDescription("Tags associated with the role") tags: List<String>,
        @GraphQLDescription("The role description prompt") role: String,
        @GraphQLDescription("The tone description prompt") tone: String,
    ): RolePrompt {
        val rolePrompt = RolePrompt(id, name, "", tags, role, tone)
        return rolePromptRepository.save(rolePrompt)
    }

    @GraphQLDescription("Deletes a role prompt.")
    suspend fun deleteRolePrompt(
        @GraphQLDescription("The unique ID of the role prompt to delete") id: String
    ): Boolean = rolePromptRepository.delete(id)
}

