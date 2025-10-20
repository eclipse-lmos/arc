// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.mcp

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.dsl.withDSLContext
import org.eclipse.lmos.arc.agents.functions.FunctionWithContext
import org.eclipse.lmos.arc.core.getOrThrow
import org.junit.jupiter.api.Test

class McpToolsTest : TestBase() {

    @Test
    fun `test list tools`(): Unit = runBlocking {
        val tools = McpTools("http://localhost:8080", null).load(null)
        assertThat(tools.map { it.name }).containsOnly("getBooks")
    }

    @Test
    fun `test execute tool`(): Unit = runBlocking {
        val tools = McpTools("http://localhost:8080", null).load(null)
        val result = tools.first { it.name == "getBooks" }.execute(mapOf("id" to "Kotlin"))
        assertThat(result.getOrThrow()).isEqualTo("""[{"name":"Spring Boot"},{"name":"Kotlin"}]""")
    }

    @Test
    fun `test execute tool with metadata`(): Unit = runBlocking {
        val tools = McpTools("http://localhost:8080", null).load(null)
        val fn = tools.first { it.name == "getBookDetails" } as FunctionWithContext
        delay(200_000)
        withDSLContext(setOf(ToolCallMetadata(data = mapOf("test" to "value")))) {
            val fnWithContext = fn.withContext(this)
            val result = fnWithContext.execute(mapOf("id" to "Kotlin"))
            assertThat(result.getOrThrow()).isEqualTo("ToolCallMetadata(data={test=value})")
        }
    }
}
