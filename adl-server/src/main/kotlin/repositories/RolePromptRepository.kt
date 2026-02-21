// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.repositories

import org.eclipse.lmos.adl.server.models.RolePrompt

/**
 * Repository for managing role prompts.
 */
interface RolePromptRepository {
    suspend fun findAll(): List<RolePrompt>
    suspend fun findById(id: String): RolePrompt?
    suspend fun save(rolePrompt: RolePrompt): RolePrompt
    suspend fun delete(id: String): Boolean
}

