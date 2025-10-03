// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.graphql.features

import dev.openfeature.sdk.FeatureProvider
import dev.openfeature.sdk.MutableContext
import dev.openfeature.sdk.OpenFeatureAPI
import org.eclipse.lmos.arc.agents.features.FeatureFlags

/**
 * Implementation of [FeatureFlags] using OpenFeature.
 */
class OpenFeatureFlags(private val featureProvider: FeatureProvider) : FeatureFlags {

    private val openFeatureAPI = OpenFeatureAPI.getInstance().also {
        it.setProviderAndWait(featureProvider)
    }

    override fun getFeature(feature: String, default: String, param: Map<String, String>): String {
        val client = openFeatureAPI.client
        return client.getStringValue(
            feature,
            default,
            MutableContext().also { context ->
                param.forEach { (key, value) ->
                    context.add(key, value)
                }
            },
        )
    }

    override fun getFeatureInt(feature: String, default: Int, param: Map<String, String>): Int {
        val client = openFeatureAPI.client
        return client.getIntegerValue(
            feature,
            default,
            MutableContext().also { context ->
                param.forEach { (key, value) ->
                    context.add(key, value)
                }
            },
        )
    }

    override fun getFeatureDouble(feature: String, default: Double, param: Map<String, String>): Double {
        val client = openFeatureAPI.client
        return client.getDoubleValue(
            feature,
            default,
            MutableContext().also { context ->
                param.forEach { (key, value) ->
                    context.add(key, value)
                }
            },
        )
    }

    override fun getFeatureBoolean(feature: String, default: Boolean, param: Map<String, String>): Boolean {
        val client = openFeatureAPI.client
        return client.getBooleanValue(
            feature,
            default,
            MutableContext().also { context ->
                param.forEach { (key, value) ->
                    context.add(key, value)
                }
            },
        )
    }
}
