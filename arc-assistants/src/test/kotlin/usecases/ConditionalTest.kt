// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConditionalTest {

    @Test
    fun `matches returns true when no conditions`() {
        val conditional = Conditional(text = "Test")
        assertThat(conditional.matches(emptySet())).isTrue
        assertThat(conditional.matches(setOf("any"))).isTrue
    }

    @Test
    fun `matches returns true if all conditions are present`() {
        val conditional = Conditional(text = "Test", conditions = setOf("foo", "bar"))
        assertThat(conditional.matches(setOf("foo", "bar", "baz"))).isTrue
    }

    @Test
    fun `matches returns false if any condition is missing`() {
        val conditional = Conditional(text = "Test", conditions = setOf("foo", "bar"))
        assertThat(conditional.matches(setOf("foo"))).isFalse
        assertThat(conditional.matches(emptySet())).isFalse
    }

    @Test
    fun `plus operator concatenates text`() {
        val conditional = Conditional(text = "Hello")
        val result = conditional + " World"
        assertThat(result.text).isEqualTo("Hello World")
        assertThat(result.conditions).isEqualTo(conditional.conditions)
    }
}
