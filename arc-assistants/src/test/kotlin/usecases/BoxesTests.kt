// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BoxesTest {

    @Test
    fun `extracts option and command from square brackets`() {
        val box = extractBox("[abc123] some text")
        assertEquals("abc123", box?.option)
        assertEquals("some text", box?.command)
    }

    @Test
    fun `returns nulls when no pattern present`() {
        val box = extractBox("No pattern here")
        assertNull(box)
    }

    @Test
    fun `extracts box with empty command`() {
        val box = extractBox("[option]")
        assertNull(box)
    }

    @Test
    fun `extracts box with only brackets`() {
        val box = extractBox("[]")
        assertNull(box)
    }

    @Test
    fun `extracts only first option and rest when multiple brackets`() {
        val box = extractBox("[eins] [zwei] Text")
        assertEquals("eins", box?.option)
        assertEquals("[zwei] Text", box?.command)
    }

    @Test
    fun `extracts empty option and command for empty brackets`() {
        val box = extractBox("[] Text")
        assertEquals("", box?.option)
        assertEquals("Text", box?.command)
    }

    @Test
    fun `returns nulls when pattern is in middle of text`() {
        val box = extractBox("Some text [value] Text")
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

        val result = extractBoxesFromText(input)
        assertEquals("Some normal text\nAnother line", result.cleanedContent)
        assertEquals(2, result.boxes.size)
        assertEquals(Box("key1", "value1"), result.boxes[0])
        assertEquals(Box("key2", "value2"), result.boxes[1])
    }

    @Test
    fun `returns all content as cleanedContent if no boxes present`() {
        val input = """
            Just some text
            Another line
        """.trimIndent()

        val result = extractBoxesFromText(input)
        assertEquals("Just some text\nAnother line", result.cleanedContent)
        assertTrue(result.boxes.isEmpty())
    }

    @Test
    fun `handles empty input`() {
        val result = extractBoxesFromText("")
        assertEquals("", result.cleanedContent)
        assertTrue(result.boxes.isEmpty())
    }

    @Test
    fun `ignores empty lines and extracts boxes correctly`() {
        val input = """
            [key] value
            
            Text
        """.trimIndent()

        val result = extractBoxesFromText(input)
        assertEquals("\nText", result.cleanedContent)
        assertEquals(1, result.boxes.size)
        assertEquals(Box("key", "value"), result.boxes[0])
    }

    @Test
    fun `extracts box with empty option and command`() {
        val input = "[] "
        val result = extractBoxesFromText(input)
        assertEquals("[] ", result.cleanedContent)
        assertEquals(0, result.boxes.size)
    }
}
