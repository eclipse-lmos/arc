// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Tests for the [parseFunctions] function.
 */
class ParseFunctionsTest {

    @Test
    fun `test parsing string with no functions`() {
        val input = "This is a string with no functions"
        val (result, functions) = input.parseFunctions()

        assertThat(result).isEqualTo(input)
        assertThat(functions).isEmpty()
    }

    @Test
    fun `test parsing string with one function`() {
        val input = "Call @my_function() to execute"
        val (result, functions) = input.parseFunctions()

        // Based on the implementation, the function should:
        // 1. Find all matches of the pattern @xxx()
        // 2. Extract both the full match and the group inside
        // 3. Replace all matches with empty string and trim the result
        assertThat(result).isEqualTo("Call my_function to execute")
        assertThat(functions).containsExactlyInAnyOrder("my_function")
    }

    @Test
    fun `test parsing string with multiple functions`() {
        val input = "First call @function1() then @function2() and finally @function3()"
        val (result, functions) = input.parseFunctions()

        // The function replaces all matches but only captures the first one in the set
        // The result is trimmed, which removes trailing spaces
        assertThat(result).isEqualTo("First call function1 then function2 and finally function3")
        assertThat(functions).containsExactlyInAnyOrder("function1", "function2", "function3")
    }

    @Test
    fun `test parsing empty string`() {
        val input = ""
        val (result, functions) = input.parseFunctions()

        assertThat(result).isEmpty()
        assertThat(functions).isEmpty()
    }

    @Test
    fun `test parsing string with function-like text that doesn't match pattern`() {
        val input = "This contains invalid functions @incomplete_function and function() but not @proper_function()"
        val (result, functions) = input.parseFunctions()

        // The regex is matching "@incomplete_function and function()" as a single match
        assertThat(result).isEqualTo("This contains invalid functions @incomplete_function and function() but not proper_function")
        assertThat(functions).containsExactlyInAnyOrder("proper_function")
    }
}
