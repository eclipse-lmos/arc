// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound.mutation

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import org.eclipse.lmos.adl.server.models.UserSettings
import org.eclipse.lmos.adl.server.repositories.UserSettingsRepository
import org.eclipse.lmos.adl.server.services.UserDefinedCompleterProvider

class UserSettingsMutation(
    private val repository: UserSettingsRepository,
    private val completerProvider: UserDefinedCompleterProvider? = null
) : Mutation {

    @GraphQLDescription("Sets the user API key and model name. Returns the settings with the API key masked (only last 3 chars visible).")
    suspend fun setUserSettings(
        @GraphQLDescription("The API key") apiKey: String,
        @GraphQLDescription("The model name") modelName: String,
        @GraphQLDescription("The model URL") modelUrl: String? = null
    ): UserSettings {
        val settings = UserSettings(apiKey = apiKey, modelName = modelName, modelUrl = modelUrl)
        repository.save(settings)
        completerProvider?.updateSettings(settings)
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
