// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.openai

import com.openai.client.OpenAIClientAsync
import com.openai.models.*
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
import java.util.*
import java.util.concurrent.CompletableFuture

class OpenAINativeClientTest {

    private val testConfig = OpenAINativeClientConfig(url = "url", apiKey = "apiKey", modelName = "modelName")

    @Test
    fun `test chatCompletions`(): Unit = runBlocking {
        val openAIClient = mockk<OpenAIClientAsync>()
        every { openAIClient.chat().completions().create(any()) } returns finalChatCompletions()

        val subject = OpenAINativeClient(testConfig, openAIClient)
        val result = subject.complete(listOf(UserMessage("Hello"))).getOrThrow()

        verify(exactly = 1) { openAIClient.chat().completions().create(any()) }
        assertThat(result.content).isEqualTo("answer")
    }

    @Test
    fun `test multiple function calls in a single response`(): Unit = runBlocking {
        val testFunction = createTestFunctionMock()

        val openAIClient = mockk<OpenAIClientAsync>()
        every { openAIClient.chat().completions().create(any()) } returnsMany listOf(
            toolsChatCompletions(listOf("testFunction", "testFunction")).toFuture(),
            finalChatCompletions(),
        )

        val subject = OpenAINativeClient(testConfig, openAIClient)
        val result = subject.complete(listOf(UserMessage("Hello")), listOf(testFunction)).getOrThrow()

        verify(exactly = 2) { openAIClient.chat().completions().create(any()) }
        coVerify(exactly = 2) { testFunction.execute(any()) }
        assertThat(result.content).isEqualTo("answer")
    }

    @Test
    fun `test multiple function calls in multiple responses`(): Unit = runBlocking {
        val testFunction = createTestFunctionMock()

        val openAIClient = mockk<OpenAIClientAsync>()
        every { openAIClient.chat().completions().create(any()) } returnsMany listOf(
            toolsChatCompletions(listOf("testFunction")).toFuture(),
            toolsChatCompletions(listOf("testFunction")).toFuture(),
            finalChatCompletions(),
        )

        val subject = OpenAINativeClient(testConfig, openAIClient)
        val result = subject.complete(listOf(UserMessage("Hello")), listOf(testFunction)).getOrThrow()

        verify(exactly = 3) { openAIClient.chat().completions().create(any()) }
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

    private fun finalChatCompletions(): CompletableFuture<ChatCompletion> {
        val chatResponseMessage = mockk<ChatCompletionMessage>()
        every { chatResponseMessage.content() } returns Optional.of("answer")

        val chatChoice = mockk<ChatCompletion.Choice>()
        every { chatChoice.finishReason() } returns ChatCompletion.Choice.FinishReason.STOP
        every { chatChoice.message() } returns chatResponseMessage

        val chatCompletions = mockk<ChatCompletion>()
        every { chatCompletions.choices() } returns listOf(chatChoice)
        return just(chatCompletions).toFuture()
    }

    private fun toolsChatCompletions(functions: List<String>): Mono<ChatCompletion> {
        val toolCalls = Optional.of(
            functions.map {
                ChatCompletionMessageToolCall
                    .builder()
                    .function(
                        ChatCompletionMessageToolCall.Function.builder()
                            .name(functions.first())
                            .arguments("")
                            .build(),
                    )
                    .id("testTool")
                    .build()
            },
        )

        val chatResponseMessage = mockk<ChatCompletionMessage>()
        every { chatResponseMessage.content() } returns Optional.of("")
        every { chatResponseMessage.toolCalls() } returns toolCalls

        val chatChoice = mockk<ChatCompletion.Choice>()
        every { chatChoice.finishReason() } returns ChatCompletion.Choice.FinishReason.TOOL_CALLS
        every { chatChoice.message() } returns chatResponseMessage
        every { chatChoice.message().toolCalls() } returns toolCalls

        val chatCompletions = mockk<ChatCompletion>()
        every { chatCompletions.choices() } returns listOf(chatChoice)
        return just(chatCompletions)
    }
}
