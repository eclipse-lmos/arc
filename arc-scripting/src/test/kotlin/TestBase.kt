// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.scripting

import io.mockk.clearMocks
import io.mockk.mockk
import org.eclipse.lmos.arc.agents.Agent
import org.eclipse.lmos.arc.agents.dsl.AgentDefinitionContext
import org.eclipse.lmos.arc.agents.dsl.BasicAgentDefinitionContext
import org.eclipse.lmos.arc.agents.dsl.ChatAgentFactory
import org.eclipse.lmos.arc.agents.dsl.CoroutineBeanProvider
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.core.getOrThrow
import org.eclipse.lmos.arc.scripting.agents.KtsAgentScriptEngine
import org.eclipse.lmos.arc.scripting.agents.ScriptingAgentLoader
import org.eclipse.lmos.arc.scripting.functions.KtsFunctionScriptEngine
import org.eclipse.lmos.arc.scripting.functions.ScriptingLLMFunctionLoader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.File

open class TestBase {

    val scripts = File("tmp-scripts")
    val scriptsSubFolder = File(scripts, "subFolder")

    val functionEngine = KtsFunctionScriptEngine()
    val agentEngine = KtsAgentScriptEngine()
    val testBeanProvider = CoroutineBeanProvider()
    val testAgentFactory = ChatAgentFactory(testBeanProvider)
    val testContext = BasicAgentDefinitionContext(testAgentFactory)
    val chatCompleter = mockk<ChatCompleter>()
    val chatCompleterProvider = ChatCompleterProvider { chatCompleter }
    val scriptingLLMFunctionLoader = ScriptingLLMFunctionLoader(testBeanProvider, functionEngine)
    val scriptingAgentLoader = ScriptingAgentLoader(testAgentFactory, agentEngine)

    fun eval(script: String, context: AgentDefinitionContext = testContext): Agent<*, *> {
        agentEngine.eval(readScript(script), context).getOrThrow()
        return testContext.agents.first()
    }

    @BeforeEach
    fun setup() {
        clearMocks(chatCompleter)
        scripts.mkdirs()
        scriptsSubFolder.mkdirs()
    }

    @AfterEach
    fun clean() {
        scripts.deleteRecursively()
    }
}
