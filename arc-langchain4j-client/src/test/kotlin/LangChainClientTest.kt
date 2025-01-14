// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.langchain4j

import org.eclipse.lmos.arc.client.langchain4j.builders.bedrockBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LangChainClientTest {

    private val langChainConfig = LangChainConfig(
        modelName = "test-model",
        url = "http://test-url",
        apiKey = "test-api-key",
        accessKeyId = "test-access-key-id",
        secretAccessKey = "test-secret-access-key",
    )

    @Test
    fun `test toString of LangChainConfig`() {
        val expected =
            "LangChainConfig(modelName=test-model, url=http://test-url, apiKey=***, accessKeyId=test-access-key-id, secretAccessKey=***)"
        assertEquals(expected, langChainConfig.toString())
    }

    @Test
    fun `test builder cache`() {
        val client1 = bedrockBuilder()(langChainConfig, null)
        val client2 = bedrockBuilder()(langChainConfig, null)
        val client3 = bedrockBuilder()(langChainConfig.copy(modelName = "newModel"), null)

        assertTrue(client1 === client2)
        assertTrue(client1 !== client3)
    }
}
