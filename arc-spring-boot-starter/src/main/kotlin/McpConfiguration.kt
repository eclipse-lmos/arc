// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring

import com.fasterxml.jackson.databind.ObjectMapper
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper
import io.modelcontextprotocol.server.McpServerFeatures
import io.modelcontextprotocol.spec.McpSchema
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.eclipse.lmos.arc.agents.dsl.BasicDSLContext
import org.eclipse.lmos.arc.agents.dsl.BeanProvider
import org.eclipse.lmos.arc.agents.dsl.CompositeBeanProvider
import org.eclipse.lmos.arc.agents.functions.FunctionWithContext
import org.eclipse.lmos.arc.agents.functions.LLMFunctionProvider
import org.eclipse.lmos.arc.agents.functions.toJsonString
import org.eclipse.lmos.arc.core.getOrThrow
import org.eclipse.lmos.arc.mcp.ToolCallMetadata
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Configuration for the MCP Client and Server resources.
 **/
@ConditionalOnClass(ToolCallbackProvider::class)
class McpConfiguration {

    private val scope = CoroutineScope(SupervisorJob())
    private val log = LoggerFactory.getLogger(McpConfiguration::class.java)

    private val mcpMapper = JacksonMcpJsonMapper(ObjectMapper())

    /**
     * Note: for some reason Spring does not yet support AsyncToolSpecifications.
     */
    @Bean
    @ConditionalOnProperty("arc.mcp.tools.expose", havingValue = "true")
    fun syncToolSpecifications(
        functionProvider: LLMFunctionProvider,
        beanProvider: BeanProvider,
        errorHandler: McpToolErrorHandler? = null,
    ): List<McpServerFeatures.SyncToolSpecification> {
        val result = AtomicReference<List<McpServerFeatures.SyncToolSpecification>>()
        val wait = Semaphore(0)
        log.info("Exposing tools over MCP...")
        scope.launch {
            result.set(
                functionProvider.provideAll().map { fn ->
                    McpServerFeatures.SyncToolSpecification.builder()
                        .tool(
                            McpSchema.Tool.builder()
                                .name(fn.name)
                                .title(fn.description)
                                .description(fn.description)
                                .meta(
                                    buildMap {
                                        fn.version?.let { put("version", it) }
                                        putAll(fn.metadata)
                                    },
                                )
                                .inputSchema(mcpMapper, fn.parameters.toJsonString())
                                .build(),

                        )
                        .callHandler { _, req ->
                            val result = AtomicReference<McpSchema.CallToolResult>()
                            val wait = Semaphore(0)
                            scope.launch {
                                log.warn("Calling MCP function: $req")
                                val args = req.arguments
                                try {
                                    val compositeBeanProvider = CompositeBeanProvider(
                                        setOf(ToolCallMetadata(req.meta ?: emptyMap())),
                                        beanProvider,
                                    )
                                    val functionResult = if (fn is FunctionWithContext) {
                                        val context = BasicDSLContext(compositeBeanProvider)
                                        fn.withContext(context).execute(args)
                                    } else {
                                        fn.execute(args)
                                    }
                                    result.set(McpSchema.CallToolResult(functionResult.getOrThrow(), false))
                                } catch (e: Exception) {
                                    log.error("Calling function ${req.name} failed with args ${req.arguments()}", e)
                                    val handledError = errorHandler?.handleToolError(req.name, e)
                                    if (handledError != null) {
                                        result.set(handledError)
                                    } else {
                                        result.set(McpSchema.CallToolResult(e.cause?.message ?: e.message, true))
                                    }
                                }
                                wait.release()
                            }
                            wait.tryAcquire(1, TimeUnit.MINUTES)
                            result.get()
                        }
                        .build()
                },
            )
            wait.release()
        }
        wait.tryAcquire(1, TimeUnit.MINUTES)
        return result.get()
    }
}

/**
 * Handler for MCP tool errors.
 */
interface McpToolErrorHandler {
    /**
     * Handle an error that occurred during a tool call.
     *
     * @param toolName The name of the tool that caused the error.
     * @param error The error.
     * @return A CallToolResult representing the error, or null to use the default error handling.
     */
    fun handleToolError(toolName: String, error: Exception): McpSchema.CallToolResult?
}
