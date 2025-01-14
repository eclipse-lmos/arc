// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.scripting

import kotlinx.coroutines.runBlocking
import org.eclipse.lmos.arc.agents.dsl.BasicFunctionDefinitionContext
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.FunctionDefinitionContext
import org.eclipse.lmos.arc.core.getOrThrow
import org.junit.jupiter.api.Test

class ContextTest : TestBase() {

    @Test
    fun `test that context receivers are supported in agents`(): Unit = runBlocking {
        val script = """
            import org.eclipse.lmos.arc.scripting.ContextFilter
            agent {
                name = "weather"
                description = ""
                systemPrompt = { "" }
                filterInput {
                     ContextFilter()
                }
            }
        """
        agentEngine.eval(script, testContext).getOrThrow()
    }

    @Test
    fun `test that context receivers are supported in functions`(): Unit = runBlocking {
        val context = BasicFunctionDefinitionContext(testBeanProvider)
        val script = """
           import org.eclipse.lmos.arc.scripting.ContextFilter
           function(
                name = "get_weather",
                description = ""
            ) { 
                ContextFilter()
                "out"
            }
        """
        functionEngine.eval(script, context).getOrThrow()
    }
}

context(DSLContext)
class ContextFilter

context(FunctionDefinitionContext)
class ContextFunction
