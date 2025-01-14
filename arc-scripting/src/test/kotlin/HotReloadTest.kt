// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.scripting

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.time.Duration.Companion.seconds

class HotReloadTest : TestBase() {

    @Test
    fun `test hot-reload created`(): Unit = runBlocking {
        val hotReload = ScriptHotReload(scriptingAgentLoader, scriptingLLMFunctionLoader, 1.seconds)

        hotReload.start(scripts)
        delay(800)

        File(scripts, "weather.agent.kts").writeText(readScript("weather.agent.kts"))
        File(scripts, "weather.functions.kts").writeText(readScript("weather.functions.kts"))
        delay(16_000)

        assertThat(scriptingAgentLoader.getAgents()).hasSize(1)
        assertThat(scriptingLLMFunctionLoader.load()).hasSize(2)
    }

    @Test
    fun `test hot-reload modified`(): Unit = runBlocking {
        val hotReload = ScriptHotReload(scriptingAgentLoader, scriptingLLMFunctionLoader, 1.seconds)

        hotReload.start(scripts)
        delay(800)

        File(scripts, "weather.agent.kts").writeText("")
        delay(10_000)
        assertThat(scriptingAgentLoader.getAgents()).hasSize(0)

        File(scripts, "weather.agent.kts").writeText(readScript("weather.agent.kts"))
        delay(10_000)
        assertThat(scriptingAgentLoader.getAgents()).hasSize(1)
    }

    // @Test
    fun `test hot-reload modified subfolder`(): Unit = runBlocking {
        val hotReload = ScriptHotReload(scriptingAgentLoader, scriptingLLMFunctionLoader, 1.seconds)

        scripts.walk().forEach { hotReload.start(it) }
        delay(800)

        File(scriptsSubFolder, "weather.agent.kts").writeText(readScript("weather.agent.kts"))
        File(scriptsSubFolder, "weather.functions.kts").writeText(readScript("weather.functions.kts"))
        delay(25_000)

        assertThat(scriptingAgentLoader.getAgents()).hasSize(1)
        assertThat(scriptingLLMFunctionLoader.load()).hasSize(2)
    }
}
