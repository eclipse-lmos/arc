// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.azure

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AzureClientConfigTest {

    @Test
    fun testToString() {
        val config = AzureClientConfig(
            modelName = "test-model",
            url = "http://test-url",
            apiKey = "test-api-key",
        )
        val expected = "AzureClientConfig(modelName=test-model, url=http://test-url, apiKey=***)"
        assertEquals(expected, config.toString())
    }
}
