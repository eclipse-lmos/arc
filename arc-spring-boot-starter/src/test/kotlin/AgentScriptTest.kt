// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatNoException
import org.eclipse.lmos.arc.agents.AgentProvider
import org.eclipse.lmos.arc.agents.functions.LLMFunctionProvider
import org.eclipse.lmos.arc.agents.getAgentByName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class AgentScriptTest {

    @Autowired
    lateinit var agentProvider: AgentProvider

    @Autowired
    lateinit var llmFunctionProvider: LLMFunctionProvider

    @Test
    fun `test agent defined as script`(): Unit = runBlocking {
        var i = 0
        while (agentProvider.getAgentByName("weather") == null && i < 30) {
            delay(1000)
            i++
        }
        assertThat(agentProvider.getAgentByName("weather")).isNotNull
    }

    @Test
    fun `test agent defined in subfolder is loaded`(): Unit = runBlocking {
        var i = 0
        while (agentProvider.getAgentByName("travel-agent") == null && i < 30) {
            delay(1000)
            i++
        }
        assertThat(agentProvider.getAgentByName("travel-agent")).isNotNull
    }

    @Test
    fun `test function defined as script`(): Unit = runBlocking {
        var i = 0

        while (i < 30) {
            try {
                llmFunctionProvider.provide("get_weather")
                break // If no exception is thrown, break the loop
            } catch (e: NoSuchElementException) {
                delay(1000)
                i++
            }
        }

        assertThatNoException().isThrownBy { (llmFunctionProvider.provide("get_weather")) }
    }

    @Test
    fun `test function defined in subfolder is loaded`(): Unit = runBlocking {
        var i = 0

        while (i < 30) {
            try {
                llmFunctionProvider.provide("get_travel_info")
                break // If no exception is thrown, break the loop
            } catch (e: NoSuchElementException) {
                delay(1000)
                i++
            }
        }

        assertThatNoException().isThrownBy { (llmFunctionProvider.provide("get_travel_info")) }
    }
}
