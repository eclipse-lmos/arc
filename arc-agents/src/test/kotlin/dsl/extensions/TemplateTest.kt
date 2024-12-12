// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.TestBase
import org.junit.jupiter.api.Test

class TemplateTest : TestBase() {

    @Test
    fun `test then extension`() {
        val predicate = true
        val result = """
           Hello${predicate then ", World!"}
       """.trim()
        assertThat(result).isEqualTo("Hello, World!")
    }

    @Test
    fun `test markdown extension`() {
        val result = listOf("one", "two", "three").markdown()
        assertThat(result).isEqualTo("- one\n- two\n- three\n")
    }

    @Test
    fun `test newline extension`() {
        val result = "Hello".newline()
        assertThat(result).isEqualTo("Hello\n")
    }
}
