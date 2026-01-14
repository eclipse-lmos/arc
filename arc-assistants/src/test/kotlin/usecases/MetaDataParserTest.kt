// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MetaDataParserTest {

    private val parser = MetaDataParser

    @Test
    fun `should extract simple headers and content`() {
        val input = """
            ---
            Key1: Value1
            Key2: Value2
            ---
            
            Here is the content.
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result.headers)
            .containsEntry("Key1", "Value1")
            .containsEntry("Key2", "Value2")
            .hasSize(2)
        assertThat(result.content).isEqualTo("Here is the content.")
    }

    @Test
    fun `should treat input without front matter delimiters as pure content`() {
        val input = """
            Key: Value
            
            Content started.
            Key2: Value2 should be part of content
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result.headers).isEmpty()
        assertThat(result.content).isEqualTo(input)
    }

    @Test
    fun `should ignore empty lines inside front matter`() {
        val input = """
            ---
            Key: Value
            
            Key2: Value2
            ---
            Content text.
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result.headers)
            .containsEntry("Key", "Value")
            .containsEntry("Key2", "Value2")
            .hasSize(2)
        assertThat(result.content).isEqualTo("Content text.")
    }

    @Test
    fun `should ignore invalid lines inside front matter`() {
        val input = """
            ---
            Key: Value
            InvalidHeaderLine
            ---
            Content text.
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result.headers)
            .containsEntry("Key", "Value")
            .hasSize(1)
        assertThat(result.content).isEqualTo("Content text.")
    }

    @Test
    fun `should handle empty input`() {
        val result = parser.parse("")

        assertThat(result.headers).isEmpty()
        assertThat(result.content).isEmpty()
    }

    @Test
    fun `should handle input with only content`() {
        val input = """
            Just some content.
            Without any headers.
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result.headers).isEmpty()
        assertThat(result.content).isEqualTo(input)
    }

    @Test
    fun `should handle input with only headers`() {
        val input = """
            ---
            Key: Value
            Key2: Value2
            ---
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result.headers)
            .containsEntry("Key", "Value")
            .containsEntry("Key2", "Value2")
            .hasSize(2)
        assertThat(result.content).isEmpty()
    }

     @Test
    fun `should trim keys and values`() {
        val input = """
            ---
            Key  :   Value  
            ---
            
            Content
        """.trimIndent()

        val result = parser.parse(input)

        assertThat(result.headers)
            .containsEntry("Key", "Value")
        assertThat(result.content).isEqualTo("Content")
    }
}
