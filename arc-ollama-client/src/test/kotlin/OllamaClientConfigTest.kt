// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.ollama

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OllamaClientConfigTest {

    @Test
    fun testToString() {
        val config = OllamaClientConfig(
            modelName = "test-model",
            url = "http://test-url",
        )
        val expected = "OllamaClientConfig(modelName=test-model, url=http://test-url)"
        assertEquals(expected, config.toString())
    }
}
