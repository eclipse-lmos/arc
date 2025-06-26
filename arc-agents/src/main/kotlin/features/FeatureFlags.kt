// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.features

/**
 * Interface for managing feature flags in the application.
 */
interface FeatureFlags {

    /**
     * Returns the value of a feature as a String.
     */
    fun getFeature(feature: String, default: String, param: Map<String, String> = emptyMap()): String

    /**
     * Returns the value of a feature as an Int.
     */
    fun getFeatureInt(feature: String, default: Int = -1, param: Map<String, String> = emptyMap()): Int

    /**
     * Returns the value of a feature as a Double.
     */
    fun getFeatureDouble(feature: String, default: Double = -1.0, param: Map<String, String> = emptyMap()): Double

    /**
     * Returns the value of a feature as a Boolean.
     */
    fun getFeatureBoolean(feature: String, default: Boolean = false, param: Map<String, String> = emptyMap()): Boolean
}
