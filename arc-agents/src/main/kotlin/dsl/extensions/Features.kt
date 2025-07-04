// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import org.eclipse.lmos.arc.agents.User
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.getOptional
import org.eclipse.lmos.arc.agents.features.FeatureFlags

/**
 * Extensions for feature flags.
 */

/**
 * Returns the value of a feature as a String.
 */
suspend fun DSLContext.getFeature(name: String, default: String, param: Map<String, String> = emptyMap()): String {
    getOptional<FeatureFlags>()?.let {
        return it.getFeature(name, default, param = getFeatureContext() + param)
    }
    getOptional<SystemContextProvider>()?.let {
        it.provideSystem().values.forEach { (k, v) ->
            if (k == "feature_$name") {
                return v
            }
        }
    }
    return default
}

/**
 * Returns the value of a feature as a String.
 */
suspend fun DSLContext.getFeatureBoolean(
    name: String,
    default: Boolean = false,
    param: Map<String, String> = emptyMap(),
): Boolean {
    getOptional<FeatureFlags>()?.let {
        return it.getFeatureBoolean(name, default, param = getFeatureContext() + param)
    }
    getOptional<SystemContextProvider>()?.let {
        it.provideSystem().values.forEach { (k, v) ->
            if (k == "feature_$name") {
                return v.toBoolean()
            }
        }
    }
    return default
}

/**
 * Creates a context map for feature flags, combining system context and user profile.
 * Warning: Fields containing "key" or "secret" are filtered out to avoid exposing sensitive information.
 */
private suspend fun DSLContext.getFeatureContext() = buildMap {
    currentAgent()?.let { agent ->
        put("agent_name", agent.name)
        put("agent_version", agent.version)
    }
    getOptional<Conversation>()?.let {
        put("session_id", it.conversationId)
    }
    getOptional<User>()?.let {
        put("user", it.id)
    }
    getOptional<SystemContextProvider>()?.provideSystem()?.values?.filter {
        !it.key.contains("key") && !it.key.contains("secret")
    }?.forEach {
        put("system_${it.key}", it.value)
    }
    getOptional<UserProfileProvider>()?.provideProfile()?.values?.filter {
        !it.key.contains("key") && !it.key.contains("secret")
    }?.forEach {
        put("user_${it.key}", it.value)
    }
}.also {
    debug("Features context created: $it")
}
