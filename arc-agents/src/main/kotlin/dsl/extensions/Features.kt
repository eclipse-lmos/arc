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
suspend fun DSLContext.isFeature(name: String, default: Boolean = false): Boolean {
    return getOptional<FeatureFlags>()?.isFeatureEnabled(name) ?: default
}
