// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring

import dev.langchain4j.model.openai.OpenAiChatModel
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class OpenAIApiTest {

    @Test
    fun `test OpenAI API configuration`(): Unit = runBlocking {
        val model = OpenAiChatModel.builder()
            .baseUrl("http://localhost:8080/openai/v1/")
            .apiKey("your-api-key")
            .modelName("gpt-4o-mini")
            .build()

        model.chat("Hello").let { response ->
            println("Response: $response")
            assertThat(response).isEqualTo("Hello from Bot!")
        }
    }
}
