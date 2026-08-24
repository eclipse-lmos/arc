// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatNoException
import org.eclipse.lmos.arc.agents.AgentProvider
import org.eclipse.lmos.arc.agents.agent.SkillDocumentParser
import org.eclipse.lmos.arc.agents.agent.SkillProvider
import org.eclipse.lmos.arc.agents.functions.LLMFunctionProvider
import org.eclipse.lmos.arc.agents.getAgentByName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class AgentBeansTest {

    @Autowired
    lateinit var agentProvider: AgentProvider

    @Autowired
    lateinit var functionProvider: LLMFunctionProvider

    @Autowired
    lateinit var skillProvider: SkillProvider

    @Test
    fun `test agent defined as bean`(): Unit = runBlocking {
        assertThat(agentProvider.getAgentByName("agentBean")).isNotNull
    }

    @Test
    fun `test function defined as bean`(): Unit = runBlocking {
        assertThatNoException().isThrownBy { runBlocking { (functionProvider.provide("get_weather_bean")) } }
    }

    @Test
    fun `test skill defined as bean`(): Unit = runBlocking {
        val skill = SkillDocumentParser.parse("writing", requireNotNull(skillProvider.load("writing")))

        assertThat(skill.description).isEqualTo("Writes concise answers.")
        assertThat(skill.instructions).isEqualTo("Use short paragraphs and clear headings.")
    }
}
