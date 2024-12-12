// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.azure

import com.azure.ai.openai.OpenAIAsyncClient
import com.azure.ai.openai.models.ChatChoice
import com.azure.ai.openai.models.ChatCompletions
import com.azure.ai.openai.models.ChatCompletionsFunctionToolCall
import com.azure.ai.openai.models.ChatResponseMessage
import com.azure.ai.openai.models.CompletionsFinishReason.STOPPED
import com.azure.ai.openai.models.CompletionsFinishReason.TOOL_CALLS
import com.azure.ai.openai.models.FunctionCall
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.ParametersSchema
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.getOrThrow
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.just

class AzureAIClientTest {

    private val testLanguageModel =
        AzureClientConfig(url = "url", apiKey = "apiKey", modelName = "modelName")

    @Test
    fun `test chatCompletions`(): Unit = runBlocking {
        val azureClient = mockk<OpenAIAsyncClient>()
        every { azureClient.getChatCompletions(testLanguageModel.modelName, any()) } returns finalChatCompletions()

        val subject = AzureAIClient(testLanguageModel, azureClient)
        val result = subject.complete(listOf(UserMessage("Hello"))).getOrThrow()

        verify(exactly = 1) { azureClient.getChatCompletions(any(), any()) }
        assertThat(result.content).isEqualTo("answer")
    }

    @Test
    fun `test multiple function calls in a single response`(): Unit = runBlocking {
        val testFunction = createTestFunctionMock()

        val azureClient = mockk<OpenAIAsyncClient>()
        every { azureClient.getChatCompletions(any(), any()) } returnsMany listOf(
            toolsChatCompletions(listOf("testFunction", "testFunction")),
            finalChatCompletions(),
        )

        val subject = AzureAIClient(testLanguageModel, azureClient)
        val result = subject.complete(listOf(UserMessage("Hello")), listOf(testFunction)).getOrThrow()

        verify(exactly = 2) { azureClient.getChatCompletions(any(), any()) }
        coVerify(exactly = 2) { testFunction.execute(any()) }
        assertThat(result.content).isEqualTo("answer")
    }

    @Test
    fun `test multiple function calls in multiple responses`(): Unit = runBlocking {
        val testFunction = createTestFunctionMock()

        val azureClient = mockk<OpenAIAsyncClient>()
        every { azureClient.getChatCompletions(any(), any()) } returnsMany listOf(
            toolsChatCompletions(listOf("testFunction")),
            toolsChatCompletions(listOf("testFunction")),
            finalChatCompletions(),
        )

        val subject = AzureAIClient(testLanguageModel, azureClient)
        val result = subject.complete(listOf(UserMessage("Hello")), listOf(testFunction)).getOrThrow()

        verify(exactly = 3) { azureClient.getChatCompletions(any(), any()) }
        coVerify(exactly = 2) { testFunction.execute(any()) }
        assertThat(result.content).isEqualTo("answer")
    }

    private fun createTestFunctionMock(): LLMFunction {
        val testFunction = mockk<LLMFunction>()
        every { testFunction.name } returns "testFunction"
        every { testFunction.description } returns "testFunction description"
        every { testFunction.parameters } returns ParametersSchema(emptyList(), emptyList())
        every { testFunction.isSensitive } returns false
        coEvery { testFunction.execute(any()) } returns Success("result")
        return testFunction
    }

    private fun finalChatCompletions(): Mono<ChatCompletions> {
        val chatResponseMessage = mockk<ChatResponseMessage>()
        every { chatResponseMessage.content } returns "answer"

        val chatChoice = mockk<ChatChoice>()
        every { chatChoice.finishReason } returns STOPPED
        every { chatChoice.message } returns chatResponseMessage

        val chatCompletions = mockk<ChatCompletions>()
        every { chatCompletions.choices } returns listOf(chatChoice)
        return just(chatCompletions)
    }

    private fun toolsChatCompletions(functions: List<String>): Mono<ChatCompletions> {
        val chatResponseMessage = mockk<ChatResponseMessage>()
        every { chatResponseMessage.content } returns ""
        every { chatResponseMessage.toolCalls } returns functions.mapIndexed { index, it ->
            ChatCompletionsFunctionToolCall("$index", FunctionCall(it, ""))
        }
        val chatChoice = mockk<ChatChoice>()
        every { chatChoice.finishReason } returns TOOL_CALLS
        every { chatChoice.message } returns chatResponseMessage

        val chatCompletions = mockk<ChatCompletions>()
        every { chatCompletions.choices } returns listOf(chatChoice)
        return just(chatCompletions)
    }
}
