// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.mcp

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.DateTimeException

class McpToolsByEnvironmentTest : TestBase() {

    private val originalProperties = mutableMapOf<String, String?>()
    private val testProperties = listOf("ARC_MCP_TOOLS_URLS", "ARC_MCP_TOOLS_CACHE_DURATION")

    @BeforeEach
    fun saveOriginalProperties() {
        testProperties.forEach { prop ->
            originalProperties[prop] = System.getProperty(prop)
        }
    }

    @AfterEach
    fun restoreOriginalProperties() {
        testProperties.forEach { prop ->
            val originalValue = originalProperties[prop]
            if (originalValue != null) {
                System.setProperty(prop, originalValue)
            } else {
                System.clearProperty(prop)
            }
        }
    }

    @Test
    fun `test loading tools from environment variables`(): Unit = runBlocking {
        // Set up environment variables
        System.setProperty("ARC_MCP_TOOLS_URLS", "http://localhost:8080")

        // Create the loader and load tools
        val loader = McpToolsByEnvironment()
        val tools = loader.load(null)

        // Verify that tools were loaded correctly
        assertThat(tools).isNotEmpty
        assertThat(tools.map { it.name }).containsOnly("getBooks")
    }

    @Test
    fun `test loading tools from multiple URLs`(): Unit = runBlocking {
        // Set up environment variables with multiple URLs
        System.setProperty("ARC_MCP_TOOLS_URLS", "http://localhost:8080, http://localhost:8080")

        // Create the loader and load tools
        val loader = McpToolsByEnvironment()
        val tools = loader.load(null)

        // Verify that tools were loaded from both URLs
        assertThat(tools).isNotEmpty
        assertThat(tools.size).isEqualTo(2) // One tool from each URL
        assertThat(tools.map { it.name }).containsOnly("getBooks")
    }

    @Test
    fun `test loading tools with cache duration`(): Unit = runBlocking {
        // Set up environment variables with cache duration
        System.setProperty("ARC_MCP_TOOLS_URLS", "http://localhost:8080")
        System.setProperty("ARC_MCP_TOOLS_CACHE_DURATION", "PT1M") // 1 minute

        // Create the loader and load tools
        val loader = McpToolsByEnvironment()
        val tools = loader.load(null)

        // Verify that tools were loaded correctly
        assertThat(tools).isNotEmpty
        assertThat(tools.map { it.name }).containsOnly("getBooks")
    }

    @Test
    fun `test empty environment variables`(): Unit = runBlocking {
        // Clear environment variables
        System.clearProperty("ARC_MCP_TOOLS_URLS")
        System.clearProperty("ARC_MCP_TOOLS_CACHE_DURATION")

        // Create the loader and load tools
        val loader = McpToolsByEnvironment()
        val tools = loader.load(null)

        // Verify that no tools were loaded
        assertThat(tools).isEmpty()
    }

    @Test
    fun `test invalid cache duration format`(): Unit = runBlocking {
        // Set up environment variables with invalid cache duration
        System.setProperty("ARC_MCP_TOOLS_URLS", "http://localhost:8080")
        System.setProperty("ARC_MCP_TOOLS_CACHE_DURATION", "invalid")

        try {
            // Create the loader and load tools - this should throw an exception
            val loader = McpToolsByEnvironment()
            loader.load(null)

            // If we get here, the test failed
            assertThat(false).isTrue().withFailMessage("Expected exception was not thrown")
        } catch (e: Exception) {
            // Verify that the exception is of the expected type
            assertThat(e).isInstanceOf(DateTimeException::class.java)
        }
    }
}
