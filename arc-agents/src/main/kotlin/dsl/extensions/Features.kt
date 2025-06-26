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
 * Returns true if a feature is set to a value other than false.
 */
suspend fun DSLContext.isFeature(name: String, default: Boolean = false): Boolean {
    getOptional<FeatureFlags>()?.let {
        return it.isFeatureEnabled(name)
    }
    getOptional<SystemContextProvider>()?.let {
        it.provideSystem().values.forEach { (k, v) ->
            if (k == "feature_$name") {
                return v != "false"
            }
        }
    }
    return default
}

/**
 * Returns the value of a feature as a String.
 */
suspend fun DSLContext.getFeature(name: String, default: String): String {
    getOptional<FeatureFlags>()?.let {
        return it.getFeature(name)
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