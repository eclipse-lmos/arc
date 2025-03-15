package org.eclipse.lmos.arc.client.openai

import com.openai.client.okhttp.OpenAIOkHttpClientAsync
import kotlinx.coroutines.runBlocking
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.core.getOrThrow
import org.junit.jupiter.api.Test

class OpenAIResponsesAPITest {

    private val testConfig = OpenAINativeClientConfig(modelName = "gpt-4o", url = "url", apiKey=System.getenv("OPENAI_API_KEY") ?: "MY_DUMMY_API_KEY")
    private val chatCompletionSettings = ChatCompletionSettings(api = "responses")

    @Test
    fun `test web search response`() = runBlocking{
        val nativeClient = OpenAINativeClient(testConfig, MockOpenAIClient(Mock.WEB_SEARCH), EventPublisherForTests())
        val response = nativeClient.complete(messages = listOf(UserMessage("What is OpenAI Responses API?")), functions = emptyList(), settings = chatCompletionSettings).getOrThrow()
        println(response)
    }
}

