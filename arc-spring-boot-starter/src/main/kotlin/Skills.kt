// SPDX-FileCopyrightText: 2026 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring

import org.eclipse.lmos.arc.agents.agent.SkillDocument
import org.eclipse.lmos.arc.agents.dsl.BasicSkillDefinitionContext
import org.eclipse.lmos.arc.agents.dsl.SkillDefinition

/**
 * Creates in-memory skill documents as Spring beans.
 *
 * ```kotlin
 * @Bean
 * fun writingSkill(skills: Skills) = skills {
 *     name = "writing"
 *     description = "Writes concise answers."
 *     "Use short paragraphs."
 * }
 * ```
 */
class Skills {

    operator fun invoke(configure: SkillDefinition.() -> String): SkillDocument {
        val context = BasicSkillDefinitionContext()
        context.skill(configure)
        return context.documents().values.single()
    }
}

