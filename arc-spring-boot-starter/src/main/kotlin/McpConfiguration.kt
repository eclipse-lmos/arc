// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.LLMFunctionProvider
import org.eclipse.lmos.arc.agents.functions.toJsonMap
import org.eclipse.lmos.arc.agents.functions.toJsonString
import org.eclipse.lmos.arc.core.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.definition.ToolDefinition
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

    @Bean
    @ConditionalOnProperty("arc.mcp.tools.expose", havingValue = "true")
    fun toolCallbackProvider(functionProvider: LLMFunctionProvider): ToolCallbackProvider {
        log.info("Exposing tools over MCP...")
        return ToolCallbackProvider {
            val result = AtomicReference<Array<ToolCallback>>()
            val wait = Semaphore(0)
            scope.launch {
                result.set(
                    functionProvider.provideAll().map {
                        FunctionToolCallback(it)
                    }.toTypedArray(),
                )
                wait.release()
            }
            wait.tryAcquire(1, TimeUnit.MINUTES)
            result.get()
        }
    }
}

/**
 * Wrapper for an LLMFunction to be used as a ToolCallback.
 */
class FunctionToolCallback(private val llmFunction: LLMFunction) : ToolCallback {

    private val scope = CoroutineScope(SupervisorJob())
    private val log = LoggerFactory.getLogger(javaClass)

    override fun call(toolInput: String): String {
        log.warn("Calling MCP function: $toolInput")
        val args = toolInput.toJsonMap()
        val result = AtomicReference<String>()
        val wait = Semaphore(0)
        scope.launch {
            val functionResult = llmFunction.execute(args)
            result.set(functionResult.getOrThrow())
            wait.release()
        }
        wait.tryAcquire(1, TimeUnit.MINUTES)
        return result.get()
    }

    override fun getToolDefinition(): ToolDefinition {
        return object : ToolDefinition {
            override fun name(): String = llmFunction.name

            override fun description(): String = llmFunction.description

            override fun inputSchema(): String = llmFunction.parameters.toJsonString()
        }
    }
}
