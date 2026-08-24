// SPDX-FileCopyrightText: 2026 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl

import org.eclipse.lmos.arc.agents.agent.DuplicateSkillNameException
import org.eclipse.lmos.arc.agents.agent.SkillDocument

@DslMarker
annotation class SkillDefinitionContextMarker

/** Defines in-memory skill documents for [org.eclipse.lmos.arc.agents.agents]. */
@SkillDefinitionContextMarker
interface SkillDefinitionContext {
    fun skill(configure: SkillDefinition.() -> String)
}

class SkillDefinition {
    var name: String? = null
    var description: String? = null
}

class BasicSkillDefinitionContext : SkillDefinitionContext {
    private val documents = linkedMapOf<String, SkillDocument>()

    override fun skill(configure: SkillDefinition.() -> String) {
        val definition = SkillDefinition()
        val instructions = definition.configure().trim()
        val name = definition.name?.takeIf { it.isNotBlank() } ?: error("Skill name is required.")
        val description = definition.description?.takeIf { it.isNotBlank() } ?: error("Skill description is required.")
        require(name.isNotBlank()) { "Skill name is required." }
        require(description.isNotBlank()) {
            "Skill description is required."
        }
        require('\n' !in name && '\r' !in name) { "Skill name must be a single line." }
        require('\n' !in description && '\r' !in description) {
            "Skill description must be a single line."
        }
        if (documents.containsKey(name)) throw DuplicateSkillNameException(name)
        documents[name] = SkillDocument(
            source = name,
            name = name,
            description = description,
            instructions = instructions,
        )
    }

    fun documents(): Map<String, SkillDocument> = documents.toMap()
}


