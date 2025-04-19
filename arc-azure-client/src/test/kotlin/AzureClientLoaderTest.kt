// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.client.azure

import com.azure.ai.openai.OpenAIAsyncClient
import com.azure.ai.openai.OpenAIClientBuilder
import com.azure.core.credential.AzureKeyCredential
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import org.eclipse.lmos.arc.agents.llm.loadConfigFromEnv
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AzureClientLoaderTest {

    private lateinit var loader: AzureClientLoader
    private lateinit var mockOpenAIClient: OpenAIAsyncClient

    @BeforeEach
    fun setUp() {
        System.setProperty("ARC_IGNORE_ARC_PROPERTIES", "true")

        // Mock the OpenAIAsyncClient to avoid actual API calls
        mockOpenAIClient = mockk(relaxed = true)

        // Mock the OpenAIClientBuilder
        mockkConstructor(OpenAIClientBuilder::class)
        every { anyConstructed<OpenAIClientBuilder>().buildAsyncClient() } returns mockOpenAIClient

        // Create the loader instance
        loader = AzureClientLoader()

        // Remove all system properties
        System.clearProperty("ARC_CLIENT")
        System.clearProperty("ARC_MODEL")
        System.clearProperty("ARC_AI_URL")
        System.clearProperty("ARC_AI_KEY")
        System.clearProperty("ARC_CLIENT[0]")
        System.clearProperty("ARC_MODEL[0]")
        System.clearProperty("ARC_AI_URL[0]")
        System.clearProperty("ARC_AI_KEY[0]")
    }

    @AfterEach
    fun tearDown() {
        // Clean up mocks
        unmockkAll()
    }

    @Test
    fun `test loadCompleter with indexed configuration`() {
        System.setProperty("ARC_CLIENT[0]", "azure")
        System.setProperty("ARC_MODEL[0]", "gpt-4")
        System.setProperty("ARC_AI_URL[0]", "https://example.azure.com")
        System.setProperty("ARC_AI_KEY[0]", "test-api-key")

        // Execute the method under test
        val result = loader.load(null, null, loadConfigFromEnv())

        // Verify the result
        assertEquals(1, result.size)
        assertNotNull(result["gpt-4"])
        assertEquals(AzureAIClient::class.java, result["gpt-4"]!!::class.java)

        // Verify that the OpenAIClientBuilder was used with the correct parameters
        verify { anyConstructed<OpenAIClientBuilder>().credential(any<AzureKeyCredential>()) }
        verify { anyConstructed<OpenAIClientBuilder>().endpoint("https://example.azure.com") }
        verify { anyConstructed<OpenAIClientBuilder>().buildAsyncClient() }
    }

    @Test
    fun `test loadCompleter with configuration`() {
        System.setProperty("ARC_CLIENT", "azure")
        System.setProperty("ARC_MODEL", "gpt-4")
        System.setProperty("ARC_AI_URL", "https://example.azure.com")
        System.setProperty("ARC_AI_KEY", "test-api-key")

        // Execute the method under test
        val result = loader.load(null, null, loadConfigFromEnv())

        // Verify the result
        assertEquals(1, result.size)
        assertNotNull(result["gpt-4"])
        assertEquals(AzureAIClient::class.java, result["gpt-4"]!!::class.java)

        // Verify that the OpenAIClientBuilder was used with the correct parameters
        verify { anyConstructed<OpenAIClientBuilder>().credential(any<AzureKeyCredential>()) }
        verify { anyConstructed<OpenAIClientBuilder>().endpoint("https://example.azure.com") }
        verify { anyConstructed<OpenAIClientBuilder>().buildAsyncClient() }
    }

    @Test
    fun `test loadCompleter with no configuration returns empty map`() {
        // Execute the method under test
        val result = loader.load(null, null, loadConfigFromEnv())

        // Verify the result
        assertEquals(0, result.size)
    }
}
