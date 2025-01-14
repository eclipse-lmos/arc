// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.openai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OpenAINativeClientConfigTest {

    @Test
    fun testToString() {
        val config = OpenAINativeClientConfig(
            modelName = "test-model",
            url = "http://test-url",
            apiKey = "test-api-key",
        )
        val expected = "OpenAINativeClientConfig(modelName=test-model, url=http://test-url, apiKey=***)"
        assertEquals(expected, config.toString())
    }
}
