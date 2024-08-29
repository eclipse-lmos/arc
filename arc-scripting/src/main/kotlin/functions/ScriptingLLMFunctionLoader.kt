// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package ai.ancf.lmos.arc.scripting.functions

import ai.ancf.lmos.arc.agents.dsl.BasicFunctionDefinitionContext
import ai.ancf.lmos.arc.agents.dsl.BeanProvider
import ai.ancf.lmos.arc.agents.functions.LLMFunction
import ai.ancf.lmos.arc.agents.functions.LLMFunctionLoader
import ai.ancf.lmos.arc.core.Result
import ai.ancf.lmos.arc.core.Success
import ai.ancf.lmos.arc.core.onFailure
import ai.ancf.lmos.arc.scripting.ScriptFailedException
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.script.experimental.api.ResultValue

class ScriptingLLMFunctionLoader(
    private val beanProvider: BeanProvider,
    private val functionScriptEngine: FunctionScriptEngine,
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
                loadFunction(script).onFailure {
                    log.warn("Failed to load functions from script: $name!", it)
                }
            }
    }
}
