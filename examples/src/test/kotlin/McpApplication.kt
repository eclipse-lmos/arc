// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.examples.mcp

import com.expediagroup.graphql.server.spring.GraphQLAutoConfiguration
import org.eclipse.lmos.arc.graphql.AgentGraphQLAutoConfiguration
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service

@SpringBootApplication(
    exclude = [
        AgentGraphQLAutoConfiguration::class,
        GraphQLAutoConfiguration::class,
    ],
)
open class McpApplication {

    @Bean
    open fun tools(bookService: BookService): ToolCallbackProvider {
        return MethodToolCallbackProvider.builder().toolObjects(bookService).build()
    }
}

@Service
open class BookService {

    @Tool(description = "Gets a list of available books")
    open fun getBooks(): List<Book> {
        return listOf(Book("Spring Boot"), Book("Kotlin"))
    }
}

data class Book(val name: String)

fun main(args: Array<String>) {
    runApplication<McpApplication>(*args)
}
