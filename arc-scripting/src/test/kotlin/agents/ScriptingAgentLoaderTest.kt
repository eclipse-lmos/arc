// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.scripting.agents

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.scripting.TestBase
import org.eclipse.lmos.arc.scripting.readScript
import org.junit.jupiter.api.Test
import java.io.File

class ScriptingAgentLoaderTest : TestBase() {

    @Test
    fun `test loading scripting agent from file`(): Unit = runBlocking {
        val scriptFile = File(scripts, "weather.agent.kts").also {
            it.writeText(readScript("weather.agent.kts"))
        }
        scriptingAgentLoader.loadAgents(scriptFile)
        assertThat(scriptingAgentLoader.getAgents()).hasSize(1)
    }
}
