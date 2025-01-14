// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.ollama

import io.ktor.server.engine.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.llm.embed
import org.eclipse.lmos.arc.core.getOrThrow
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
