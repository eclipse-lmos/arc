// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.mcp

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.dsl.extensions.NoPromptFoundException
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.getOrThrow
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import java.util.*

class McpPromptRetrieverTest : TestBase() {

    @LocalServerPort
    private var port: Int = 0

    @Test
    fun `test prompt retriever`(): Unit = runBlocking {
        val prompt = McpPromptRetriever("http://localhost:$port").fetchPromptText("greeting", emptyMap())
        prompt.getOrThrow().let {
            assertThat(it).isEqualTo("Hello friend! How can I assist you today?")
        }
    }

    @Test
    fun `test for NoPromptFoundException`(): Unit = runBlocking {
        val result = McpPromptRetriever("http://localhost:$port").fetchPromptText("no-prompt", emptyMap()) as Failure
        assertThat(result.reason is NoPromptFoundException).isTrue()
    }
}
