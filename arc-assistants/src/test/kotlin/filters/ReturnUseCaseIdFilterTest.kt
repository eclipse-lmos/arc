// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.filters

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.dsl.AgentFilter
import org.eclipse.lmos.arc.agents.dsl.withDSLContext
import org.eclipse.lmos.arc.assistants.support.TestBase
import org.junit.jupiter.api.Test

class ReturnUseCaseIdFilterTest : TestBase() {

    @Test
    fun `test ReturnUseCaseIdFilter implements AgentFilter`() = runBlocking {
        // Verify that ReturnUseCaseIdFilter implements AgentFilter
        withDSLContext {
            assertThat(ReturnUseCaseIdFilter::class.java).isAssignableTo(AgentFilter::class.java)
        }
    }
}
