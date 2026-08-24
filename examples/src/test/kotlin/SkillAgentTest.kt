// SPDX-FileCopyrightText: 2026 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.examples.skills

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SkillAgentTest {

    @Test
    fun `activates the inline skill through the automatic tool`(): Unit = runBlocking {
        val response = runSkillAgent()

        assertThat(response).contains("# Release notes")
        assertThat(response).contains("do not invent version numbers")
        assertThat(response).doesNotContain("name: release-notes")
    }
}

