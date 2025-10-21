// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases

data class FlowOptions(val contentWithoutOptions: String, val options: List<FlowOption>)

data class FlowOption(val option: String, val command: String)

/**
 * Extracts all boxes from a multi-line string and returns a FlowOptions instance.
 *
 * Each line starting with [option] is considered a box and is extracted.
 * The cleanedContent field contains the original text without lines containing boxes.
 *
 * @param text the multi-line input string
 * @return a Boxes instance with cleaned content and a list of extracted boxes
 */
fun extractFlowOptions(text: String): FlowOptions {
    val boxes = mutableListOf<FlowOption>()
    val cleanedLines = mutableListOf<String>()

    text.lines().forEach { line ->
        val box = extractFlowOption(line)
        if (box != null) {
            boxes.add(box)
        } else {
            cleanedLines.add(line)
        }
    }

    val cleanedContent = cleanedLines.joinToString("\n")
    return FlowOptions(cleanedContent, boxes)
}

/**
 * Extracts a FlowOption from a line of text.
 *
 * A FlowOption is defined as a string starting with [option] followed by instructions.
 * Returns a Box instance if the pattern matches, otherwise null.
 *
 * @param text the input string to extract from
 * @return a Box if the pattern matches, otherwise null
 */
fun extractFlowOption(text: String): FlowOption? {
    val regex = Regex("""^\[(.*?)\]\s*([^(]+)""")
    val match = regex.find(text.trim())
    return if (match != null) {
        val option = match.groups[1]?.value ?: ""
        val command = match.groups[2]?.value?.takeIf { it.isNotBlank() } ?: return null
        FlowOption(option, command)
    } else {
        null
    }
}

/**
 * Extension function to extract FlowOptions from a UseCase's formatted string.
 *
 * @param conditions a set of conditions to format the UseCase
 * @return a FlowOptions instance extracted from the UseCase's formatted string
 */
suspend fun UseCase.flowOptions(conditions: Set<String>): FlowOptions {
    return extractFlowOptions(this.formatToString(conditions = conditions))
}
