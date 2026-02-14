// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.repositories

import org.eclipse.lmos.adl.server.models.UserSettings

interface UserSettingsRepository {
    suspend fun save(settings: UserSettings): UserSettings
    suspend fun get(): UserSettings?
}

