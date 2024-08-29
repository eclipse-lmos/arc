// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package ai.ancf.lmos.arc.client.ollama

import ai.ancf.lmos.arc.agents.conversation.UserMessage
import ai.ancf.lmos.arc.agents.llm.embed
import ai.ancf.lmos.arc.core.getOrThrow
import io.ktor.server.engine.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OllamaClientTest : TestBase() {

    @Test
    fun `test chat completion text`(): Unit = runBlocking {
        val client = OllamaClient(OllamaClientConfig(modelName = "llama3:8b", url = "http://localhost:$port"))
        val message = client.complete(listOf(UserMessage("test question"))).getOrThrow()
        assertThat(message.content).isEqualTo("answer to test")
    }

    @Test
    fun `test embed text`(): Unit = runBlocking {
        val client = OllamaClient(OllamaClientConfig(modelName = "llama3:8b", url = "http://localhost:$port"))
        val embedding = client.embed("Hello, world!").getOrThrow().embedding
        assertThat(embedding).containsExactly(0.0, 0.1)
    }
}
