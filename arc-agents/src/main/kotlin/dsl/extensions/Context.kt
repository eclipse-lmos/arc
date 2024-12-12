// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.get

/**
 * Provides access to the system context.
 */
suspend fun DSLContext.system(key: String, defaultValue: String? = null): String {
    return get<SystemContextProvider>().provideSystem().values[key]
        ?: defaultValue
        ?: kotlin.error("System context key not found: $key!")
}

/**
 * Provides access to the user profile.
 */
suspend fun DSLContext.userProfile(key: String, defaultValue: String? = null): String {
    return get<UserProfileProvider>().provideProfile().values[key]
        ?: defaultValue
        ?: kotlin.error("Profile key not found: $key!")
}

/**
 * Data classes for system context and user profile.
 */
data class SystemContext(val values: Map<String, String>)
data class UserProfile(val values: Map<String, String>)

/**
 * Providers. Implement these interfaces to provide the system context and user profile.
 */
interface SystemContextProvider {
    fun provideSystem(): SystemContext
}

interface UserProfileProvider {
    fun provideProfile(): UserProfile
}
