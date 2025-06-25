// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.features

import dev.openfeature.contrib.providers.flagd.FlagdProvider
import dev.openfeature.sdk.OpenFeatureAPI
import org.eclipse.lmos.arc.agents.TestBase
import org.junit.jupiter.api.Test

class FeatureFlagsTest : TestBase() {

    @Test
    fun `test feature flags`() {
        val openFeatureAPI = OpenFeatureAPI.getInstance()
        openFeatureAPI.setProviderAndWait(FlagdProvider())

        val client = openFeatureAPI.client
        println(client.getBooleanValue("welcome-message", false))
    }
}
