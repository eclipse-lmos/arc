// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.graphql.context

import org.eclipse.lmos.arc.agents.dsl.extensions.SystemContext
import org.eclipse.lmos.arc.agents.dsl.extensions.SystemContextProvider
import org.eclipse.lmos.arc.agents.dsl.extensions.UserProfile
import org.eclipse.lmos.arc.agents.dsl.extensions.UserProfileProvider
import org.eclipse.lmos.arc.api.AgentRequest

/**
 * Provides the system context and user profile to the context of the DSL.
 */
data class ContextProvider(val request: AgentRequest) : SystemContextProvider, UserProfileProvider {

    override fun provideSystem(): SystemContext {
        return SystemContext(request.systemContext.associate { it.key to it.value })
    }

    override fun provideProfile(): UserProfile {
        return UserProfile(request.userContext.profile.associate { it.key to it.value })
    }
}
