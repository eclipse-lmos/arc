// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases

data class Boxes(val cleanedContent: String, val boxes: List<Box>)

data class Box(val option: String, val command: String)

/**
 * Extracts a box from the beginning of the given text.
 *
 * A box is defined as a string starting with [option] followed by optional text.
 * Returns a Box instance if the pattern matches, otherwise null.
 *
 * @param text the input string to extract from
 * @return a Box if the pattern matches, otherwise null
 */
fun extractBox(text: String): Box? {
    val regex = Regex("""^\[(.*?)\]\s*(.*)""")
    val match = regex.find(text)
    return if (match != null) {
        val option = match.groups[1]?.value ?: ""
        val command = match.groups[2]?.value?.takeIf { it.isNotBlank() } ?: return null
        Box(option, command)
    } else {
        null
    }
}

/**
 * Extracts all boxes from a multi-line string and returns a Boxes instance.
 *
 * Each line starting with [option] is considered a box and is extracted.
 * The cleanedContent field contains the original text without lines containing boxes.
 *
 * @param text the multi-line input string
 * @return a Boxes instance with cleaned content and a list of extracted boxes
 */
fun extractBoxesFromText(text: String): Boxes {
    val boxes = mutableListOf<Box>()
    val cleanedLines = mutableListOf<String>()

    text.lines().forEach { line ->
        val box = extractBox(line)
        if (box != null) {
            boxes.add(box)
        } else {
            cleanedLines.add(line)
        }
    }

    val cleanedContent = cleanedLines.joinToString("\n")
    return Boxes(cleanedContent, boxes)
}
