// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.features

/**
 * Interface for managing feature flags in the application.
 */
interface FeatureFlags {

    fun isFeatureEnabled(feature: String): Boolean
}
