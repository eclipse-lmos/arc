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
        System.clearProperty("ARC_AZURE_MODEL_NAME")
        System.clearProperty("ARC_AZURE_ENDPOINT")
        System.clearProperty("ARC_AZURE_API_KEY")
        System.clearProperty("ARC_AZURE[0]_MODEL_NAME")
        System.clearProperty("ARC_AZURE[0]_ENDPOINT")
        System.clearProperty("ARC_AZURE[0]_API_KEY")
        System.clearProperty("ARC_CLIENT")
        System.clearProperty("ARC_MODEL")
        System.clearProperty("ARC_AI_URL")
        System.clearProperty("ARC_AI_KEY")
    }

    @AfterEach
    fun tearDown() {
        // Clean up mocks
        unmockkAll()
    }

    @Test
    fun `test loadCompleter with Azure configuration`() {
        System.setProperty("ARC_AZURE_MODEL_NAME", "gpt-4")
        System.setProperty("ARC_AZURE_ENDPOINT", "https://example.azure.com")
        System.setProperty("ARC_AZURE_API_KEY", "test-api-key")

        // Execute the method under test
        val result = loader.load(null, null, emptyList())

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
    fun `test loadCompleter with indexed Azure configuration`() {
        System.setProperty("ARC_AZURE[0]_MODEL_NAME", "gpt-4")
        System.setProperty("ARC_AZURE[0]_ENDPOINT", "https://example.azure.com")
        System.setProperty("ARC_AZURE[0]_API_KEY", "test-api-key")

        // Execute the method under test
        val result = loader.load(null, null, emptyList())

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
    fun `test loadCompleter with legacy configuration`() {
        System.setProperty("ARC_CLIENT", "azure")
        System.setProperty("ARC_MODEL", "gpt-4")
        System.setProperty("ARC_AI_URL", "https://example.azure.com")
        System.setProperty("ARC_AI_KEY", "test-api-key")

        // Execute the method under test
        val result = loader.load(null, null, emptyList())

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
        val result = loader.load(null, null, emptyList())

        // Verify the result
        assertEquals(0, result.size)
    }
}
