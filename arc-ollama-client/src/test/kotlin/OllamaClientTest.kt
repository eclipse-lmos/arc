// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package io.github.lmos.arc.client.ollama

import io.github.lmos.arc.agents.conversation.UserMessage
import io.github.lmos.arc.agents.llm.embed
import io.github.lmos.arc.core.getOrThrow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OllamaClientTest : TestBase() {

    @Test
    fun `test chat completion text`(): Unit = runBlocking {
        val client = OllamaClient(OllamaClientConfig(modelName = "llama3:8b", url = "http://localhost:8080"))
        val message = client.complete(listOf(UserMessage("test question"))).getOrThrow()
        assertThat(message.content).isEqualTo("answer to test")
    }

    @Test
    fun `test embed text`(): Unit = runBlocking {
        val client = OllamaClient(OllamaClientConfig(modelName = "llama3:8b", url = "http://localhost:8080"))
        val embedding = client.embed("Hello, world!").getOrThrow().embedding
        assertThat(embedding).containsExactly(0.0, 0.1)
    }
}
