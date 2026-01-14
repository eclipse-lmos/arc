// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases

/**
 * Extracts header fields from a markdown file and separates them from the body content.
 * Headers are expected to be at the top of the file in "Key: Value" format,
 * enclosed by "---" lines (YAML front matter style).
 */
object MetaDataParser {

    data class ParseResult(
        val meta: Map<String, String>,
        val content: String,
    )

    fun parse(input: String): ParseResult {
        val lines = input.lines()
        val headers = mutableMapOf<String, String>()
        val contentBuilder = StringBuilder()
        var parsingHeaders = false

        if (lines.isNotEmpty() && lines[0].trim() == "---") {
            parsingHeaders = true
        }

        for ((index, line) in lines.withIndex()) {
            if (index == 0 && parsingHeaders) continue

            if (parsingHeaders) {
                if (line.trim() == "---") {
                    parsingHeaders = false
                    continue
                }

                val separatorIndex = line.indexOf(':')
                if (separatorIndex > 0) {
                    val key = line.substring(0, separatorIndex).trim()
                    val value = line.substring(separatorIndex + 1).trim()
                    headers[key] = value
                }
            } else {
                contentBuilder.append(line).append("\n")
            }
        }

        return ParseResult(headers, contentBuilder.toString().trim())
    }
}
