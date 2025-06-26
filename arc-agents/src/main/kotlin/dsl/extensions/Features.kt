// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.getOptional
import org.eclipse.lmos.arc.agents.features.FeatureFlags

/**
 * Extensions for feature flags.
 */

/**
 * Returns the value of a feature as a String.
 */
suspend fun DSLContext.getFeature(name: String, default: String): String {
    getOptional<FeatureFlags>()?.let {
        return it.getFeature(name, default, param = getFeatureContext())
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
suspend fun DSLContext.getFeatureBoolean(name: String, default: Boolean = false): Boolean {
    getOptional<FeatureFlags>()?.let {
        return it.getFeatureBoolean(name, default, param = getFeatureContext())
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
 */
private suspend fun DSLContext.getFeatureContext() = buildMap {
    getOptional<SystemContextProvider>()?.provideSystem()?.values?.forEach {
        put("system_${it.key}", it.value)
    }
    getOptional<UserProfileProvider>()?.provideProfile()?.values?.forEach {
        put("user_${it.key}", it.value)
    }
}
