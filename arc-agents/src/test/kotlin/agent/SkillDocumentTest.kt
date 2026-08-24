// SPDX-FileCopyrightText: 2026 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.agent

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class SkillDocumentTest {

    @Test
    fun `parses frontmatter and excludes it from instructions`() {
        val document = SkillDocumentParser.parse(
            "writing",
            """
            ---
            name: writing
            description: "Writes concise answers."
            ---

            # Writing
            Use short sentences.
            """.trimIndent(),
        )

        assertThat(document.name).isEqualTo("writing")
        assertThat(document.description).isEqualTo("Writes concise answers.")
        assertThat(document.instructions).isEqualTo("# Writing\nUse short sentences.")
    }

    @Test
    fun `rejects documents without required frontmatter`() {
        assertThatThrownBy { SkillDocumentParser.parse("writing", "# Writing") }
            .isInstanceOf(InvalidSkillDocumentException::class.java)
            .hasMessageContaining("missing opening frontmatter delimiter")
    }
}

