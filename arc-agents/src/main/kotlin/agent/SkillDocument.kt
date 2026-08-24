// SPDX-FileCopyrightText: 2026 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.agent

/** A validated skill document loaded from a [SkillProvider]. */
data class SkillDocument(
    val source: String,
    val name: String,
    val description: String,
    val instructions: String,
) {
    fun toSkill() = Skill(id = name, name = name, description = description)

    /** Renders this document in the Frontmatter format accepted by [SkillDocumentParser]. */
    fun toContent() = """
        ---
        name: $name
        description: $description
        ---

        $instructions
    """.trimIndent()
}

/** Parses the required YAML frontmatter of a skill document. */
object SkillDocumentParser {

    fun parse(source: String, content: String): SkillDocument {
        val normalized = content.removePrefix("\uFEFF").replace("\r\n", "\n")
        if (!normalized.startsWith("---\n")) throw InvalidSkillDocumentException(source, "missing opening frontmatter delimiter")

        val closingDelimiter = normalized.indexOf("\n---", startIndex = 4)
        if (closingDelimiter < 0) throw InvalidSkillDocumentException(source, "missing closing frontmatter delimiter")
        val afterDelimiter = closingDelimiter + 4
        if (afterDelimiter < normalized.length && normalized[afterDelimiter] != '\n') {
            throw InvalidSkillDocumentException(source, "invalid closing frontmatter delimiter")
        }

        val metadata = normalized.substring(4, closingDelimiter)
            .lineSequence()
            .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
            .associate { line ->
                val separator = line.indexOf(':')
                if (separator <= 0) throw InvalidSkillDocumentException(source, "invalid frontmatter entry '$line'")
                line.substring(0, separator).trim() to line.substring(separator + 1).trim().unquote()
            }
        val name = metadata["name"].orEmpty().takeIf { it.isNotBlank() }
            ?: throw InvalidSkillDocumentException(source, "missing required frontmatter field 'name'")
        val description = metadata["description"].orEmpty().takeIf { it.isNotBlank() }
            ?: throw InvalidSkillDocumentException(source, "missing required frontmatter field 'description'")
        val instructions = normalized.substring(afterDelimiter).removePrefix("\n").trim()
        return SkillDocument(source, name, description, instructions)
    }

    private fun String.unquote(): String =
        if (length >= 2 && first() == last() && first() in setOf('\'', '"')) substring(1, length - 1) else this
}

class InvalidSkillDocumentException(source: String, reason: String) :
    Exception("Invalid skill document '$source': $reason")

class DuplicateSkillNameException(name: String) : Exception("Multiple skill documents declare the name '$name'")

