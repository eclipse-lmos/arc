// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.mcp

import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptRegistration
import io.modelcontextprotocol.spec.McpSchema
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult
import io.modelcontextprotocol.spec.McpSchema.PromptArgument
import io.modelcontextprotocol.spec.McpSchema.PromptMessage
import io.modelcontextprotocol.spec.McpSchema.Role
import io.modelcontextprotocol.spec.McpSchema.TextContent
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication(proxyBeanMethods = false)
open class TestApplication {

    @Bean
    open fun tools(): ToolCallbackProvider {
        return MethodToolCallbackProvider
            .builder()
            .toolObjects(AuthorRepository())
            .build()
    }

    @Bean
    open fun prompts(): List<SyncPromptRegistration> {
        val prompt = McpSchema.Prompt(
            "greeting",
            "A friendly greeting prompt",
            listOf(PromptArgument("name", "The name to greet", true)),
        )

        val promptRegistration = SyncPromptRegistration(
            prompt,
        ) { getPromptRequest: GetPromptRequest ->
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
    fun getBooks(): List<Book> {
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
