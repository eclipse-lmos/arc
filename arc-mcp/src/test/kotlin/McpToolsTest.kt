// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.mcp

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.core.getOrThrow
import org.junit.jupiter.api.Test
import java.util.*

class McpToolsTest : TestBase() {

    @Test
    fun `test list tools`(): Unit = runBlocking {
        val tools = McpTools("http://localhost:8080").load()
        assertThat(tools.map { it.name }).containsOnly("getBooks", "getAuthors")
    }

    @Test
    fun `test execute tool`(): Unit = runBlocking {
        val tools = McpTools("http://localhost:8080").load()
        val result = tools.first().execute(emptyMap())
        assertThat(result.getOrThrow()).isEqualTo("""[{"name":"Spring Boot"},{"name":"Kotlin"}]""")
    }
}
