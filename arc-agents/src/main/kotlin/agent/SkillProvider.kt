// SPDX-FileCopyrightText: 2026 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.agent

import org.eclipse.lmos.arc.agents.dsl.extensions.localFile
import org.eclipse.lmos.arc.agents.dsl.extensions.localResource

/**
 * Loads a complete Frontmatter-based agent skill document.
 *
 * Register an implementation as a DSL context bean to retrieve skills from a
 * custom source. Documents must declare non-empty `name` and `description`
 * Frontmatter fields.
 */
fun interface SkillProvider {
    suspend fun load(name: String): String?
}

/**
 * Default [SkillProvider] for skills packaged with the application or stored
 * on the local filesystem.
 */
class FileClasspathSkillProvider : SkillProvider {
    override suspend fun load(name: String): String? {
        return loadLocal("skills/$name/SKILL.md") ?: loadLocal("$name/SKILL.md") ?: loadLocal(name)
    }

    private fun loadLocal(path: String) = localResource(path) ?: localFile(path)
}

/** Resolves inline skills first and delegates missing skills to one fallback provider. */
class CompositeSkillProvider(
    private val skills: Map<String, SkillDocument>,
    private val fallback: SkillProvider,
) : SkillProvider {
    override suspend fun load(name: String): String? = skills[name]?.toContent() ?: fallback.load(name)
}

/** Thrown when an agent declares a skill that its [SkillProvider] cannot load. */
class SkillNotFoundException(name: String) : Exception("No skill found under name: $name")


