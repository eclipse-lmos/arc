// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.langchain4j

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.llm.AIClientConfig
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.llm.CompleterLoaderService
import org.eclipse.lmos.arc.agents.llm.OutputFormat
import org.eclipse.lmos.arc.agents.llm.ReasoningEffort
import org.eclipse.lmos.arc.client.langchain4j.builders.azureOpenAiBuilder
import org.eclipse.lmos.arc.client.langchain4j.builders.openAiBuilder
import org.eclipse.lmos.arc.client.langchain4j.loaders.AzureOpenAiClientLoader
import org.eclipse.lmos.arc.client.langchain4j.loaders.OpenAiClientLoader
import org.junit.jupiter.api.Test
import java.util.ServiceLoader

class ClientBuildersTest {

    @Test
    fun `OpenAI builder maps completion settings including service tier`() {
        val config = AIClientConfig(
            client = "langchain4j-openai",
            modelName = "gpt-5",
            endpoint = "https://api.openai.com/v1",
            apiKey = "test-key",
        )
        val settings = ChatCompletionSettings(
            temperature = 0.2,
            topP = 0.8,
            maxTokens = 512,
            format = OutputFormat.JSON,
            reasoningEffort = ReasoningEffort.HIGH,
            serviceTier = "flex",
        )

        val model = openAiBuilder()(config, settings)
        val parameters = model.defaultRequestParameters()

        assertThat(model::class.simpleName).isEqualTo("OpenAiResponsesChatModel")
        assertThat(parameters.modelName()).isEqualTo("gpt-5")
        assertThat(parameters.temperature()).isEqualTo(0.2)
        assertThat(parameters.topP()).isEqualTo(0.8)
        assertThat(parameters.maxOutputTokens()).isEqualTo(512)
        assertThat(parameters.get("serviceTier")).isEqualTo("flex")
        assertThat(parameters.get("reasoningEffort")).isEqualTo("high")
        assertThat(parameters.get("parallelToolCalls")).isEqualTo(true)
        assertThat(parameters.get("maxToolCalls")).isEqualTo(1000)
        assertThat(parameters.responseFormat()).isEqualTo(dev.langchain4j.model.chat.request.ResponseFormat.JSON)
    }

    @Test
    fun `Azure builder maps supported completion settings`() {
        val config = AIClientConfig(
            client = "langchain4j-azure",
            modelName = "gpt-4o",
            endpoint = "https://example.openai.azure.com",
            apiKey = "test-key",
        )
        val settings = ChatCompletionSettings(
            temperature = 0.3,
            topP = 0.7,
            maxTokens = 256,
            format = OutputFormat.JSON,
        )

        val model = azureOpenAiBuilder()(config, settings)
        val parameters = model.defaultRequestParameters()

        assertThat(model::class.simpleName).isEqualTo("AzureOpenAiChatModel")
        assertThat(parameters.modelName()).isEqualTo("gpt-4o")
        assertThat(parameters.temperature()).isEqualTo(0.3)
        assertThat(parameters.topP()).isEqualTo(0.7)
        assertThat(parameters.maxOutputTokens()).isEqualTo(256)
        assertThat(parameters.responseFormat()).isEqualTo(dev.langchain4j.model.chat.request.ResponseFormat.JSON)
    }

    @Test
    fun `loaders accept namespaced client names`() {
        val openAiClients = OpenAiClientLoader().load(
            tracer = null,
            eventPublisher = null,
            configs = listOf(AIClientConfig(client = "langchain4j-openai", modelName = "gpt-5", apiKey = "key")),
        )
        val azureClients = AzureOpenAiClientLoader().load(
            tracer = null,
            eventPublisher = null,
            configs = listOf(
                AIClientConfig(
                    client = "langchain4j-azure",
                    modelName = "gpt-4o",
                    endpoint = "https://example.openai.azure.com",
                    apiKey = "key",
                ),
            ),
        )

        assertThat(openAiClients).containsKey("gpt-5")
        assertThat(azureClients).containsKey("gpt-4o")
    }

    @Test
    fun `service loader discovers OpenAI and Azure loaders`() {
        val loaderTypes = ServiceLoader.load(CompleterLoaderService::class.java)
            .map { it::class.java }

        assertThat(loaderTypes).contains(OpenAiClientLoader::class.java, AzureOpenAiClientLoader::class.java)
    }

    private fun Any.get(property: String): Any? = javaClass.getMethod(property).invoke(this)
}


