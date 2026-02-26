// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound.query

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import org.eclipse.lmos.adl.server.models.UserSettings
import org.eclipse.lmos.adl.server.repositories.UserSettingsRepository

class UserSettingsQuery(
    private val repository: UserSettingsRepository
) : Query {

    @GraphQLDescription("Retrieves the user settings. The API key is masked.")
    suspend fun userSettings(): UserSettings? {
        val settings = repository.get() ?: return null
        return maskApiKey(settings)
    }

    private fun maskApiKey(settings: UserSettings): UserSettings {
        val key = settings.apiKey
        val maskedKey = if (key.length <= 3) {
            key
        } else {
            "..." + key.takeLast(3)
        }
        return settings.copy(apiKey = maskedKey)
    }
}

