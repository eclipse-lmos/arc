// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.AgentProvider
import org.eclipse.lmos.arc.agents.getAgentByName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "arc.scripts.enabled=false",
    ],
)
class GenAgentTest {

    @Autowired
    lateinit var agentProvider: AgentProvider

    @Test
    fun `test gen agent is loaded`(): Unit = runBlocking {
        assertThat(agentProvider.getAgentByName("weather-gen")).isNotNull
    }
}
