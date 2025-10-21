// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.assistants.support.usecases.validation

import org.eclipse.lmos.arc.agents.dsl.extensions.localResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UseCaseToJsonTest {

    private val useCaseToJson = UseCaseToJson()

    @Test
    fun `parse returns UseCaseResult with expected use case count`() {
        val input = localResource("use_cases.md")!!
        val result = useCaseToJson.parse(input)
        assertEquals(3, result.useCaseCount)
        assertTrue(result.errors?.isEmpty() ?: true)
    }

    @Test
    fun `parse returns errors`() {
        val input = localResource("use_cases_errors.md")!!
        val result = useCaseToJson.parse(input)
        assertEquals(3, result.useCaseCount)
        assertEquals(2, result.errors!!.size)
        assertEquals("Duplicate use case IDs found: usecase1", result.errors[0].message)
        assertEquals("The same examples were used in multiple use cases: usecase1, usecase3", result.errors[1].message)
    }
}
