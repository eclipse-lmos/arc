// SPDX-FileCopyrightText: 2026 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.eclipse.lmos.arc.agents.agent.DuplicateSkillNameException
import org.junit.jupiter.api.Test

class SkillDefinitionContextTest {

    @Test
    fun `collects multiple inline skills in declaration order`() {
        val context = BasicSkillDefinitionContext()
        context.skill {
            name = "release-notes"
            description = "Creates release notes."
            "Release note instructions"
        }
        context.skill {
            name = "review"
            description = "Reviews changes."
            "Review instructions"
        }

        assertThat(context.documents().keys).containsExactly("release-notes", "review")
        with(context.documents().getValue("review")) {
            assertThat(source).isEqualTo("review")
            assertThat(description).isEqualTo("Reviews changes.")
            assertThat(instructions).isEqualTo("Review instructions")
        }
    }

    @Test
    fun `rejects duplicate inline skill names`() {
        val context = BasicSkillDefinitionContext()
        context.skill {
            name = "review"
            description = "Reviews changes."
            "Instructions"
        }

        assertThatThrownBy {
            context.skill {
                name = "review"
                description = "Another review."
                "Instructions"
            }
        }.isInstanceOf(DuplicateSkillNameException::class.java)
    }
}

