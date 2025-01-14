// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.scripting.functions

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.scripting.TestBase
import org.eclipse.lmos.arc.scripting.readScript
import org.junit.jupiter.api.Test
import java.io.File

class ScriptingLLMFunctionLoaderTest : TestBase() {

    @Test
    fun `test loading scripting functions from file`(): Unit = runBlocking {
        val scriptFile = File(scripts, "weather.functions.kts").also {
            it.writeText(readScript("weather.functions.kts"))
        }
        scriptingLLMFunctionLoader.loadFunctions(scriptFile)
        assertThat(scriptingLLMFunctionLoader.load()).hasSize(2)
    }
}
