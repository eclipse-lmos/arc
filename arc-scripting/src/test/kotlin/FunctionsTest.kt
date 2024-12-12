// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.scripting

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.dsl.BasicFunctionDefinitionContext
import org.junit.jupiter.api.Test

class FunctionsTest : TestBase() {

    @Test
    fun `test script`(): Unit = runBlocking {
        val context = BasicFunctionDefinitionContext(testBeanProvider)
        functionEngine.eval(
            """
           function(
              name = "get_weather",
              description = "the weather service",
              params = types(string("location","the location"))
             ) {
                  "the weather is weather in location!"
              }
        """,
            context,
        )
        assertThat(context.functions).hasSize(1)
    }
}
