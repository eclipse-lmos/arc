// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.server.ktor

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.agents
import org.junit.jupiter.api.Test

class ServerTest {

    @Test
    fun `test creating simple agent`(): Unit = runBlocking {
        val agents = agents {
            agent {
                name = "test"
              prompt {
                  """Prompt"""
              }
            }
        }
        agents.serve()
    }

}
