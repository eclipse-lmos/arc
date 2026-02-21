// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.repositories.impl

import org.eclipse.lmos.adl.server.models.UserSettings
import org.eclipse.lmos.adl.server.repositories.UserSettingsRepository
import java.util.concurrent.atomic.AtomicReference

class InMemoryUserSettingsRepository : UserSettingsRepository {
    private val storage = AtomicReference<UserSettings?>()

    override suspend fun save(settings: UserSettings): UserSettings {
        storage.set(settings)
        return settings
    }

    override suspend fun get(): UserSettings? {
        return storage.get()
    }
}

