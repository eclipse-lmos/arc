// SPDX-FileCopyrightText: 2026 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.agent

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files

class SkillProviderTest {

    @Test
    fun `default provider reads a skill from the filesystem`(): Unit = runBlocking {
        val skill = Files.createTempFile("arc-skill", ".md")
        try {
            Files.writeString(skill, "Use short answers.")

            assertThat(FileClasspathSkillProvider().load(skill.toString())).isEqualTo("Use short answers.")
        } finally {
            Files.deleteIfExists(skill)
        }
    }

    @Test
    fun `default provider resolves logical names from the skills folder`(): Unit = runBlocking {
        assertThat(FileClasspathSkillProvider().load("release-notes"))
            .contains("name: release-notes")
    }

    @Test
    fun `composite provider resolves inline skills before its fallback`(): Unit = runBlocking {
        val fallback = SkillProvider { "fallback: $it" }
        val provider = CompositeSkillProvider(
            mapOf(
                "writing" to SkillDocument(
                    source = "writing",
                    name = "writing",
                    description = "Writes answers.",
                    instructions = "inline: writing",
                ),
            ),
            fallback,
        )

        assertThat(provider.load("writing")).contains("inline: writing")
        assertThat(provider.load("external")).isEqualTo("fallback: external")
    }
}

