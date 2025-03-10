// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.mcp

import io.modelcontextprotocol.spec.McpError
import io.modelcontextprotocol.spec.McpSchema
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest
import io.modelcontextprotocol.spec.McpSchema.TextContent
import kotlinx.coroutines.reactor.awaitSingle
import org.eclipse.lmos.arc.agents.dsl.extensions.NoPromptFoundException
import org.eclipse.lmos.arc.agents.dsl.extensions.PromptException
import org.eclipse.lmos.arc.agents.dsl.extensions.PromptRetriever
import org.eclipse.lmos.arc.agents.dsl.extensions.PromptServerException
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.mapFailure
import org.slf4j.LoggerFactory
import java.io.Closeable

/**
 * PromptRetriever that uses the MCP client to fetch prompts.
 */
class McpPromptRetriever(private val url: String) : PromptRetriever, Closeable {

    private val log = LoggerFactory.getLogger(javaClass)
    private val clientBuilder = McpClientBuilder(url)

    override suspend fun fetchPromptText(name: String, args: Map<String, Any?>): Result<String, PromptException> =
        clientBuilder.execute { client ->
            val prompt = client.getPrompt(GetPromptRequest(name, args)).awaitSingle()
            log.debug("Fetched prompt: $name description: ${prompt.description} from $url")
            prompt.toText()
        }.mapFailure { it.toException(name) }

    /**
     * Converts exceptions thrown by the MCP client to a PromptException.
     */
    private fun Exception.toException(promptName: String) = when (this) {
        is McpError -> {
            if (this.jsonRpcError.code == -32603) {
                NoPromptFoundException(promptName)
            } else {
                PromptServerException(promptName, this)
            }
        }

        else -> PromptServerException(promptName, this)
    }

    /**
     * Converts the prompt result to a string.
     */
    private fun McpSchema.GetPromptResult.toText() = messages.map { it.content }
        .joinToString(separator = "\n") { if (it is TextContent) it.text else it.toString() }

    override fun close() {
        clientBuilder.close()
    }
}
