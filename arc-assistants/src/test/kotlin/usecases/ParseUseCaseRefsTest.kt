// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Tests for the [parseUseCaseRefs] function.
 */
class ParseUseCaseRefsTest {

    @Test
    fun `test parsing string with no use case references`() {
        val input = "This is a string with no use case references"
        val (result, useCaseRefs) = input.parseUseCaseRefs()

        assertThat(result).isEqualTo(input)
        assertThat(useCaseRefs).isEmpty()
    }

    @Test
    fun `test parsing string with one use case reference`() {
        val input = "Go to use case #other_usecase for more information"
        val (result, useCaseRefs) = input.parseUseCaseRefs()

        assertThat(result).isEqualTo("Go to use case #other_usecase for more information")
        assertThat(useCaseRefs).containsExactlyInAnyOrder("other_usecase")
    }

    @Test
    fun `test parsing string with multiple use case references`() {
        val input = "First go to #usecase1 then #usecase2 and finally #usecase3"
        val (result, useCaseRefs) = input.parseUseCaseRefs()

        assertThat(result).isEqualTo("First go to #usecase1 then #usecase2 and finally #usecase3")
        assertThat(useCaseRefs).containsExactlyInAnyOrder("usecase1", "usecase2", "usecase3")
    }

    @Test
    fun `test parsing empty string`() {
        val input = ""
        val (result, useCaseRefs) = input.parseUseCaseRefs()

        assertThat(result).isEmpty()
        assertThat(useCaseRefs).isEmpty()
    }

    @Test
    fun `test parsing string with use case-like text that doesn't match pattern`() {
        val input = "This contains invalid references like #incomplete-id and usecase# but not #proper_usecase"
        val (result, useCaseRefs) = input.parseUseCaseRefs()

        assertThat(result).isEqualTo("This contains invalid references like #incomplete-id and usecase# but not #proper_usecase")
        assertThat(useCaseRefs).containsExactlyInAnyOrder("incomplete-id", "proper_usecase")
    }

    @Test
    fun `test parsing string with use case references containing hyphens and numbers`() {
        val input = "Go to #use-case-1 and #use-case-2. *#06#"
        val (result, useCaseRefs) = input.parseUseCaseRefs()

        assertThat(result).isEqualTo("Go to #use-case-1 and #use-case-2. *#06#")
        assertThat(useCaseRefs).containsExactlyInAnyOrder("use-case-1", "use-case-2")
    }

    @Test
    fun `test parsing string with use case references containing path`() {
        val input = "Go to #file/use-case-1 and #file/file/use-case-2."
        val (result, useCaseRefs) = input.parseUseCaseRefs()

        assertThat(result).isEqualTo("Go to #use-case-1 and #use-case-2.")
        assertThat(useCaseRefs).containsExactlyInAnyOrder("file/use-case-1", "file/file/use-case-2")
    }
}
