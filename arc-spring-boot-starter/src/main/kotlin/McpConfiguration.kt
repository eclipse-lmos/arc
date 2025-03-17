// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.LLMFunctionProvider
import org.eclipse.lmos.arc.agents.functions.toJsonString
import org.eclipse.lmos.arc.core.getOrNull
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
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

    @Bean
    fun toolCallbackProvider(functionProvider: LLMFunctionProvider): ToolCallbackProvider {
        return object : ToolCallbackProvider {

            override fun getToolCallbacks(): Array<ToolCallback> {
                val result = AtomicReference<Array<ToolCallback>>()
                val wait = Semaphore(1)
                scope.launch {
                    result.set(functionProvider.provideAll().map {
                        FunctionToolCallback(it)
                    }.toTypedArray())
                }
                println("Thread: ${Thread.currentThread().isVirtual}")
                wait.tryAcquire(1, TimeUnit.MINUTES) // TODO
                return result.get()
            }
        }
    }
}

class FunctionToolCallback(private val llmFunction: LLMFunction) : ToolCallback {

    private val scope = CoroutineScope(SupervisorJob())

    override fun call(toolInput: String): String {
        println("Calling function: ${toolInput}")
        val args = emptyMap<String, Any>()
        val result = AtomicReference<String>()
        val wait = Semaphore(1)
        scope.launch {
            val r = llmFunction.execute(args)
            result.set(r.getOrNull() ?: "") // TODO: handle error
        }
        println("Thread: ${Thread.currentThread().isVirtual}")
        wait.tryAcquire(1, TimeUnit.MINUTES) // TODO
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
