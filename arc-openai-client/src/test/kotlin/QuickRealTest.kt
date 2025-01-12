// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.openai

import com.openai.client.okhttp.OpenAIOkHttpClientAsync
import com.openai.core.JsonValue
import com.openai.models.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.eclipse.lmos.arc.agents.functions.*
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.Success

fun main() {
    val client = OpenAIOkHttpClientAsync.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .build()

    val functions = listOf(
        object : LLMFunction {
            override val name: String
                get() = "get_current_weather"
            override val parameters: ParametersSchema
                get() = ParametersSchema(
                    listOf("location", "unit"),
                    listOf(
                        ParameterSchema(
                            name = "location",
                            description = "The city and state, e.g., San Francisco, CA.",
                            type = ParameterType("string"),
                            enum = emptyList(),
                        ),
                        ParameterSchema(
                            name = "unit",
                            description = "The temperature unit to use.",
                            type = ParameterType("string"),
                            enum = listOf("celsius", "fahrenheit"),
                        ),
                    ),
                )
            override val description: String
                get() = "Retrieve the current weather for a specified location."
            override val group: String?
                get() = null
            override val isSensitive: Boolean
                get() = false

            override suspend fun execute(input: Map<String, Any?>): Result<String, LLMFunctionException> {
                return Success("test")
            }
        },
        object : LLMFunction {
            override val name: String
                get() = "dummy"
            override val parameters: ParametersSchema
                get() = ParametersSchema(
                    emptyList(),
                    listOf(
                        ParameterSchema(
                            name = "locations",
                            description = "The list of cities and states, e.g., San Francisco, CA.",
                            type = ParameterType("array", items = ParameterType("string")),
                            enum = emptyList(),
                        ),
                    ),
                )
            override val description: String
                get() = "Dummy test function."
            override val group: String?
                get() = null
            override val isSensitive: Boolean
                get() = false

            override suspend fun execute(input: Map<String, Any?>): Result<String, LLMFunctionException> {
                return Success("test")
            }
        },
    )

    val settings = ChatCompletionSettings()

    val params = ChatCompletionCreateParams.builder()
        .messages(
            listOf(
                ChatCompletionMessageParam.ofChatCompletionDeveloperMessageParam(
                    ChatCompletionDeveloperMessageParam.builder()
                        .role(ChatCompletionDeveloperMessageParam.Role.DEVELOPER)
                        .content(ChatCompletionDeveloperMessageParam.Content.ofTextContent("You are a helpful assistant."))
                        .build(),
                ),
                ChatCompletionMessageParam.ofChatCompletionUserMessageParam(
                    ChatCompletionUserMessageParam.builder()
                        .role(ChatCompletionUserMessageParam.Role.USER)
                        .content(ChatCompletionUserMessageParam.Content.ofTextContent("What's the weather like in New York today?"))
                        .build(),
                ),
            ),
        )
        .tools(toOpenAIFunctions(functions) ?: emptyList())
        .model(ChatModel.GPT_4O_MINI)
        .apply {
            settings.temperature?.let { temperature(it) }
        }
        .build()

    val chatCompletion = try {
        runBlocking {
            client.chat().completions().create(params).await()
        }
    } catch (ex: Exception) {
        println(ex)
        return
    }
    println(chatCompletion)
    val message = chatCompletion.choices().get(0).message()
    val finishReason = chatCompletion.choices().get(0).finishReason()
    println(message)
    println(finishReason)
}

private fun toOpenAIFunctions(functions: List<LLMFunction>) = functions.map { fn ->
    val jsonObject = fn.parameters.toOpenAISchemaAsMap()
    println("$jsonObject")

    ChatCompletionTool.builder()
        .type(ChatCompletionTool.Type.FUNCTION)
        .function(
            FunctionDefinition.builder()
                .name(fn.name).description(fn.description).parameters(
                    FunctionParameters.builder().putAdditionalProperty("type", JsonValue.from(jsonObject["type"]))
                        .putAdditionalProperty("properties", JsonValue.from(jsonObject["properties"]))
                        .putAdditionalProperty("required", JsonValue.from(jsonObject["required"])).build(),
                ).build(),
        ).build()
}.takeIf { it.isNotEmpty() }
