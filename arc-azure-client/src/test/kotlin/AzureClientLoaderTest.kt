package org.eclipse.lmos.arc.client.azure

import com.azure.ai.openai.OpenAIAsyncClient
import com.azure.ai.openai.OpenAIClientBuilder
import com.azure.core.credential.AzureKeyCredential
import com.azure.core.credential.KeyCredential
import com.azure.core.credential.TokenCredential
import com.azure.identity.DefaultAzureCredential
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.eclipse.lmos.arc.agents.env.getEnvironmentValue
import org.eclipse.lmos.arc.agents.llm.ANY_MODEL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AzureClientLoaderTest {

    private lateinit var loader: AzureClientLoader
    private lateinit var mockOpenAIClient: OpenAIAsyncClient
    private lateinit var mockBuilder: OpenAIClientBuilder

    @BeforeEach
    fun setUp() {
        // Mock the OpenAIAsyncClient to avoid actual API calls
        mockOpenAIClient = mockk(relaxed = true)

        // Mock the OpenAIClientBuilder
        mockBuilder = mockk(relaxed = true)
        every { mockBuilder.buildAsyncClient() } returns mockOpenAIClient

        // Mock the getEnvironmentValue function
        mockkStatic("org.eclipse.lmos.arc.agents.env.EnvironmentCompleterLoaderKt")

        // Mock the OpenAIClientBuilder constructor
        mockkStatic("com.azure.ai.openai.OpenAIClientBuilder")
        every { OpenAIClientBuilder() } returns mockBuilder

        // Create the loader instance
        loader = AzureClientLoader()
    }

    @AfterEach
    fun tearDown() {
        // Clean up mocks
        unmockkAll()
    }

    @Test
    fun `test loadCompleter with Azure configuration`() {
        // Mock environment variables
        every { getEnvironmentValue("ARC_AZURE_MODEL_NAME") } returns "gpt-4"
        every { getEnvironmentValue("ARC_AZURE_ENDPOINT") } returns "https://example.azure.com"
        every { getEnvironmentValue("ARC_AZURE_API_KEY") } returns "test-api-key"

        // For the indexed environment variables (ARC_AZURE[i]_*)
        for (i in 0..9) {
            every { getEnvironmentValue("ARC_AZURE[$i]_MODEL_NAME") } returns null
        }

        // For legacy properties
        every { getEnvironmentValue("ARC_CLIENT") } returns null

        // For OpenAI properties
        every { getEnvironmentValue("ARC_OPENAI_API_KEY") } returns null
        every { getEnvironmentValue("OPENAI_API_KEY") } returns null

        // Execute the method under test
        val result = loader.load(null, null)

        // Verify the result
        assertEquals(1, result.size)
        assertNotNull(result["gpt-4"])
        assertEquals(AzureAIClient::class.java, result["gpt-4"]!!::class.java)

        // Verify that the OpenAIClientBuilder was used with the correct parameters
        verify { mockBuilder.credential(any<AzureKeyCredential>()) }
        verify { mockBuilder.endpoint("https://example.azure.com") }
        verify { mockBuilder.buildAsyncClient() }
    }

    @Test
    fun `test loadCompleter with indexed Azure configuration`() {
        // Mock environment variables for the main Azure config
        every { getEnvironmentValue("ARC_AZURE_MODEL_NAME") } returns null
        every { getEnvironmentValue("ARC_AZURE_ENDPOINT") } returns null
        every { getEnvironmentValue("ARC_AZURE_API_KEY") } returns null

        // Mock environment variables for indexed Azure config
        every { getEnvironmentValue("ARC_AZURE[0]_MODEL_NAME") } returns "gpt-4"
        every { getEnvironmentValue("ARC_AZURE[0]_ENDPOINT") } returns "https://example.azure.com"
        every { getEnvironmentValue("ARC_AZURE[0]_API_KEY") } returns "test-api-key"

        // For the rest of the indexed environment variables
        for (i in 1..9) {
            every { getEnvironmentValue("ARC_AZURE[$i]_MODEL_NAME") } returns null
        }

        // For legacy properties
        every { getEnvironmentValue("ARC_CLIENT") } returns null

        // For OpenAI properties
        every { getEnvironmentValue("ARC_OPENAI_API_KEY") } returns null
        every { getEnvironmentValue("OPENAI_API_KEY") } returns null

        // Execute the method under test
        val result = loader.load(null, null)

        // Verify the result
        assertEquals(1, result.size)
        assertNotNull(result["gpt-4"])
        assertEquals(AzureAIClient::class.java, result["gpt-4"]!!::class.java)

        // Verify that the OpenAIClientBuilder was used with the correct parameters
        verify { mockBuilder.credential(any<AzureKeyCredential>()) }
        verify { mockBuilder.endpoint("https://example.azure.com") }
        verify { mockBuilder.buildAsyncClient() }
    }

    @Test
    fun `test loadCompleter with legacy configuration`() {
        // Mock environment variables for the main Azure config
        every { getEnvironmentValue("ARC_AZURE_MODEL_NAME") } returns null
        every { getEnvironmentValue("ARC_AZURE_ENDPOINT") } returns null
        every { getEnvironmentValue("ARC_AZURE_API_KEY") } returns null

        // For the indexed environment variables
        for (i in 0..9) {
            every { getEnvironmentValue("ARC_AZURE[$i]_MODEL_NAME") } returns null
        }

        // Mock legacy environment variables
        every { getEnvironmentValue("ARC_CLIENT") } returns "azure"
        every { getEnvironmentValue("ARC_MODEL") } returns "gpt-4"
        every { getEnvironmentValue("ARC_AI_URL") } returns "https://example.azure.com"
        every { getEnvironmentValue("ARC_AI_KEY") } returns "test-api-key"

        // For OpenAI properties
        every { getEnvironmentValue("ARC_OPENAI_API_KEY") } returns null
        every { getEnvironmentValue("OPENAI_API_KEY") } returns null

        // Execute the method under test
        val result = loader.load(null, null)

        // Verify the result
        assertEquals(1, result.size)
        assertNotNull(result["gpt-4"])
        assertEquals(AzureAIClient::class.java, result["gpt-4"]!!::class.java)

        // Verify that the OpenAIClientBuilder was used with the correct parameters
        verify { mockBuilder.credential(any<AzureKeyCredential>()) }
        verify { mockBuilder.endpoint("https://example.azure.com") }
        verify { mockBuilder.buildAsyncClient() }
    }

    @Test
    fun `test loadCompleter with OpenAI configuration`() {
        // Mock environment variables for Azure configs
        every { getEnvironmentValue("ARC_AZURE_MODEL_NAME") } returns null
        every { getEnvironmentValue("ARC_AZURE_ENDPOINT") } returns null
        every { getEnvironmentValue("ARC_AZURE_API_KEY") } returns null

        // For the indexed environment variables
        for (i in 0..9) {
            every { getEnvironmentValue("ARC_AZURE[$i]_MODEL_NAME") } returns null
        }

        // For legacy properties
        every { getEnvironmentValue("ARC_CLIENT") } returns null

        // Mock OpenAI environment variables
        every { getEnvironmentValue("ARC_OPENAI_API_KEY") } returns "test-openai-key"
        every { getEnvironmentValue("OPENAI_API_KEY") } returns null

        // Execute the method under test
        val result = loader.load(null, null)

        // Verify the result
        assertEquals(1, result.size)
        assertNotNull(result[ANY_MODEL])
        assertEquals(AzureAIClient::class.java, result[ANY_MODEL]!!::class.java)

        // Verify that the OpenAIClientBuilder was used with the correct parameters
        verify { mockBuilder.credential(any<KeyCredential>()) }
        verify { mockBuilder.buildAsyncClient() }
    }

    @Test
    fun `test loadCompleter with fallback to OPENAI_API_KEY`() {
        // Mock environment variables for Azure configs
        every { getEnvironmentValue("ARC_AZURE_MODEL_NAME") } returns null
        every { getEnvironmentValue("ARC_AZURE_ENDPOINT") } returns null
        every { getEnvironmentValue("ARC_AZURE_API_KEY") } returns null

        // For the indexed environment variables
        for (i in 0..9) {
            every { getEnvironmentValue("ARC_AZURE[$i]_MODEL_NAME") } returns null
        }

        // For legacy properties
        every { getEnvironmentValue("ARC_CLIENT") } returns null

        // Mock OpenAI environment variables
        every { getEnvironmentValue("ARC_OPENAI_API_KEY") } returns null
        every { getEnvironmentValue("OPENAI_API_KEY") } returns "test-openai-key"

        // Execute the method under test
        val result = loader.load(null, null)

        // Verify the result
        assertEquals(1, result.size)
        assertNotNull(result[ANY_MODEL])
        assertEquals(AzureAIClient::class.java, result[ANY_MODEL]!!::class.java)

        // Verify that the OpenAIClientBuilder was used with the correct parameters
        verify { mockBuilder.credential(any<KeyCredential>()) }
        verify { mockBuilder.buildAsyncClient() }
    }

    @Test
    fun `test loadCompleter with no configuration returns empty map`() {
        // Mock environment variables to return null
        every { getEnvironmentValue(any()) } returns null

        // Execute the method under test
        val result = loader.load(null, null)

        // Verify the result
        assertEquals(0, result.size)
    }

    @Test
    fun `test loadCompleter with endpoint but no API key uses DefaultAzureCredential`() {
        // Mock environment variables
        every { getEnvironmentValue("ARC_AZURE_MODEL_NAME") } returns "gpt-4"
        every { getEnvironmentValue("ARC_AZURE_ENDPOINT") } returns "https://example.azure.com"
        every { getEnvironmentValue("ARC_AZURE_API_KEY") } returns null

        // For the indexed environment variables (ARC_AZURE[i]_*)
        for (i in 0..9) {
            every { getEnvironmentValue("ARC_AZURE[$i]_MODEL_NAME") } returns null
        }

        // For legacy properties
        every { getEnvironmentValue("ARC_CLIENT") } returns null

        // For OpenAI properties
        every { getEnvironmentValue("ARC_OPENAI_API_KEY") } returns null
        every { getEnvironmentValue("OPENAI_API_KEY") } returns null

        // Mock DefaultAzureCredential
        mockkStatic("com.azure.identity.DefaultAzureCredentialBuilder")
        val mockCredential = mockk<DefaultAzureCredential>()
        every { com.azure.identity.DefaultAzureCredentialBuilder().build() } returns mockCredential

        // Execute the method under test
        val result = loader.load(null, null)

        // Verify the result
        assertEquals(1, result.size)
        assertNotNull(result["gpt-4"])
        assertEquals(AzureAIClient::class.java, result["gpt-4"]!!::class.java)

        // Verify that the OpenAIClientBuilder was used with the correct parameters
        verify { mockBuilder.credential(any<TokenCredential>()) }
        verify { mockBuilder.endpoint("https://example.azure.com") }
        verify { mockBuilder.buildAsyncClient() }
    }
}
