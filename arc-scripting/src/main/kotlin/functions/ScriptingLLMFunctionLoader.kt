// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.scripting.functions

import org.eclipse.lmos.arc.agents.dsl.BasicFunctionDefinitionContext
import org.eclipse.lmos.arc.agents.dsl.BeanProvider
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.LLMFunctionLoader
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.onFailure
import org.eclipse.lmos.arc.scripting.ScriptFailedException
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.script.experimental.api.ResultValue

class ScriptingLLMFunctionLoader(
    private val beanProvider: BeanProvider,
    private val functionScriptEngine: FunctionScriptEngine = KtsFunctionScriptEngine(),
    private val eventPublisher: EventPublisher? = null,
) : LLMFunctionLoader {

    private val log = LoggerFactory.getLogger(javaClass)
    private val functions = ConcurrentHashMap<String, LLMFunction>()

    override fun load(): List<LLMFunction> {
        return functions.values.toList()
    }

    fun deleteFunctions() {
        functions.clear()
    }

    /**
     * Loads the functions defined in an Agent DSL script.
     */
    fun loadFunction(agentScript: String): Result<ResultValue?, ScriptFailedException> {
        val context = BasicFunctionDefinitionContext(beanProvider)
        val result = functionScriptEngine.eval(agentScript, context)

        if (result is Success && context.functions.isNotEmpty()) {
            log.info("Discovered the following llm functions (scripting): ${context.functions.joinToString { it.name }}")
            functions.putAll(context.functions.associateBy { "${it.group}::${it.name}" })
        }
        return result
    }

    /**
     * Loads the functions defined in a list of Agent DSL script files.
     */
    fun loadFunctions(vararg files: File) {
        files
            .asSequence()
            .filter { it.name.endsWith(".functions.kts") }
            .map { it.name to it.readText() }
            .forEach { (name, script) ->
                val result = loadFunction(script).onFailure {
                    log.warn("Failed to load functions from script: $name!", it)
                }
                when (result) {
                    is Success -> eventPublisher?.publish(FunctionLoadedEvent(name))
                    is Failure -> eventPublisher?.publish(
                        FunctionLoadedEvent(
                            name,
                            result.reason.message ?: "Unknown error!",
                        ),
                    )
                }
            }
    }

    /**
     * Loads the agent functions located in the given folder.
     */
    fun loadAgentsFromFolder(folder: File) {
        folder.walk().filter { it.isFile }.forEach { loadFunctions(it) }
    }
}
