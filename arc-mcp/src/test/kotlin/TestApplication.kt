// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.mcp

import io.modelcontextprotocol.server.McpServerFeatures
import io.modelcontextprotocol.server.McpSyncServerExchange
import io.modelcontextprotocol.spec.McpSchema
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult
import io.modelcontextprotocol.spec.McpSchema.PromptArgument
import io.modelcontextprotocol.spec.McpSchema.PromptMessage
import io.modelcontextprotocol.spec.McpSchema.Role
import io.modelcontextprotocol.spec.McpSchema.TextContent
import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.dsl.boolean
import org.eclipse.lmos.arc.agents.dsl.get
import org.eclipse.lmos.arc.agents.dsl.string
import org.eclipse.lmos.arc.agents.dsl.types
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.core.result
import org.eclipse.lmos.arc.spring.Functions
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication(proxyBeanMethods = false)
open class TestApplication {

    @Bean
    open fun getBooks(function: Functions) = function(
        name = "getBooks",
        description = "description",
        params = types(string("id", "the id")),
    ) { (id) ->
        """[{"name":"Spring Boot"},{"name":"$id"}]"""
    }

    @Bean
    open fun getBookDetails(function: Functions) = function(
        name = "getBookDetails",
        description = "description",
        version = "1.0.0",
        params = types(string("id", "the id"), boolean("readonly", "flag")),
    ) { (id) ->
        val meta = get<ToolCallMetadata>()
        meta.toString()
    }

    @Bean
    open fun chatCompleterProvider() = ChatCompleterProvider {
        object : ChatCompleter {
            override suspend fun complete(
                messages: List<ConversationMessage>,
                functions: List<LLMFunction>?,
                settings: ChatCompletionSettings?,
            ) = result<AssistantMessage, ArcException> {
                functions?.forEach { function ->
                    function.execute(mapOf("param" to "test"))
                }
                AssistantMessage("Hello!")
            }
        }
    }

    @Bean
    open fun prompts(): List<McpServerFeatures.SyncPromptSpecification> {
        val prompt = McpSchema.Prompt(
            "greeting",
            "A friendly greeting prompt",
            listOf(PromptArgument("name", "The name to greet", true)),
        )

        val promptRegistration = McpServerFeatures.SyncPromptSpecification(
            prompt,
        ) { exchange: McpSyncServerExchange, getPromptRequest: GetPromptRequest ->
            var nameArgument = getPromptRequest.arguments()["name"] as String?
            if (nameArgument == null) {
                nameArgument = "friend"
            }
            val userMessage =
                PromptMessage(Role.USER, TextContent("Hello $nameArgument! How can I assist you today?"))
            GetPromptResult("A personalized greeting message", listOf(userMessage))
        }

        return listOf(promptRegistration)
    }
}

class AuthorRepository {

    @Tool(description = "Gets a list of available books")
    fun getBooks(@ToolParam(description = "A description") id: String?): List<Book> {
        return listOf(Book("Spring Boot"), Book("Kotlin"))
    }

    @Tool(description = "Gets a list of available books")
    fun getBooksByBoolean(@ToolParam(description = "A description") id: Boolean): List<Book> {
        return listOf(Book("Spring Boot"), Book("Kotlin"))
    }

    @Tool(description = "Gets a list of available books")
    fun getBooksByObject(
        @ToolParam(description = "A description") id: String,
        @ToolParam(description = "A description") data: Data,
    ): List<Book> {
        return listOf(Book("Spring Boot"), Book("Kotlin"))
    }

    @Tool(description = "Gets a list of available books")
    fun getBooksByEnum(
        @ToolParam(description = "A description") enum: Enums,
    ): List<Book> {
        return listOf(Book("Spring Boot"), Book("Kotlin"))
    }

    @Tool(description = "Gets a list of available books")
    fun getBooksByArray(
        @ToolParam(description = "A description") list: List<String>,
    ): List<Book> {
        return listOf(Book("Spring Boot"), Book("Kotlin"))
    }

    @Tool(description = "Get a list of authors")
    fun getAuthors(): List<Author> = listOf(
        Author("Pat"),
        Author("Matt"),
    )
}

data class Book(val name: String)
data class Author(val name: String)
data class Data(val name: String)
enum class Enums {
    A,
    B,
    C,
}
