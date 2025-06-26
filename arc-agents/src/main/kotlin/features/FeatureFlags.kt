// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.features

/**
 * Interface for managing feature flags in the application.
 */
interface FeatureFlags {

    /**
     * Returns true if a feature is set to a value other than false.
     */
    fun isFeatureEnabled(feature: String): Boolean

    /**
     * Returns the value of a feature as a String.
     */
    fun getFeature(feature: String): String
}
