// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.dsl.AgentFilter
import org.eclipse.lmos.arc.agents.dsl.withDSLContext
import org.eclipse.lmos.arc.assistants.support.filters.LLMHackingDetector
import org.junit.jupiter.api.Test

class LLMHackingDetectorTest : TestBase() {

    @Test
    fun `test LLMHackingDetector implements AgentFilter`() = runBlocking {
        // Verify that LLMHackingDetector implements AgentFilter
        withDSLContext {
            val detector = LLMHackingDetector()
            assertThat(detector).isInstanceOf(AgentFilter::class.java)
        }
    }
}
