// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ExtractFlowOptionTest {

    @Test
    fun `extracts option and command from square brackets`() {
        val box = extractFlowOption("[abc123] some text")
        assertEquals("abc123", box?.option)
        assertEquals("some text", box?.command)
    }

    @Test
    fun `returns nulls when no pattern present`() {
        val box = extractFlowOption("No pattern here")
        assertNull(box)
    }

    @Test
    fun `extracts box with empty command`() {
        val box = extractFlowOption("[option]")
        assertNull(box)
    }

    @Test
    fun `extracts box with only brackets`() {
        val box = extractFlowOption("[]")
        assertNull(box)
    }

    @Test
    fun `extracts only first option and rest when multiple brackets`() {
        val box = extractFlowOption("[eins] [zwei] Text")
        assertEquals("eins", box?.option)
        assertEquals("[zwei] Text", box?.command)
    }

    @Test
    fun `extracts option with whitespaces`() {
        val box = extractFlowOption("     [eins] command")
        assertEquals("eins", box?.option)
    }

    @Test
    fun `ignores links`() {
        var box = extractFlowOption("[link text] (https://example.com)")
        assertNull(box)
        box = extractFlowOption("[link text](https://example.com)")
        assertNull(box)
    }

    @Test
    fun `extracts empty option and command for empty brackets`() {
        val box = extractFlowOption("[] Text")
        assertEquals("", box?.option)
        assertEquals("Text", box?.command)
    }

    @Test
    fun `returns nulls when pattern is in middle of text`() {
        val box = extractFlowOption("Some text [value] Text")
        assertNull(box)
    }

    @Test
    fun `extracts multiple boxes and cleaned content`() {
        val input = """
            [key1] value1
            Some normal text
            [key2] value2
            Another line
        """.trimIndent()

        val result = extractFlowOptions(input)
        assertEquals("Some normal text\nAnother line", result.contentWithoutOptions)
        assertEquals(2, result.options.size)
        assertEquals(FlowOption("key1", "value1"), result.options[0])
        assertEquals(FlowOption("key2", "value2"), result.options[1])
    }

    @Test
    fun `returns all content as cleanedContent if no boxes present`() {
        val input = """
            Just some text
            Another line
        """.trimIndent()

        val result = extractFlowOptions(input)
        assertEquals("Just some text\nAnother line", result.contentWithoutOptions)
        assertTrue(result.options.isEmpty())
    }

    @Test
    fun `handles empty input`() {
        val result = extractFlowOptions("")
        assertEquals("", result.contentWithoutOptions)
        assertTrue(result.options.isEmpty())
    }

    @Test
    fun `ignores empty lines and extracts boxes correctly`() {
        val input = """
            [key] value
            
            Text
        """.trimIndent()

        val result = extractFlowOptions(input)
        assertEquals("\nText", result.contentWithoutOptions)
        assertEquals(1, result.options.size)
        assertEquals(FlowOption("key", "value"), result.options[0])
    }

    @Test
    fun `extracts box with empty option and command`() {
        val input = "[] "
        val result = extractFlowOptions(input)
        assertEquals("[] ", result.contentWithoutOptions)
        assertEquals(0, result.options.size)
    }
}
