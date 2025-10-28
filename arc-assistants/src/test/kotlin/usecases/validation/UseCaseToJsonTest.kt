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
        assertEquals("The same examples were used in multiple Use Cases: [usecase1, usecase3] Examples: [- Example 1, - Example 2]", result.errors[1].message)
    }

    @Test
    fun `parse example errors`() {
        val result = useCaseToJson.parse(
            """
            ### UseCase: usecase1 (1)  
            #### Description
            The description 
            #### Solution
            Primary Solution
            #### Examples
            - Example 1
            ----
            ### UseCase: usecase2 (1) 
            #### Description
            The description
            #### Solution
            Primary Solution
            #### Examples
            - Example 1
            ----""",
        )
        assertEquals(1, result.errors!!.size)
        assertEquals("The same examples were used in multiple Use Cases: [usecase1, usecase2] Examples: [- Example 1]", result.errors[0].message)
    }

    @Test
    fun `parse example errors - error with conditionals`() {
        val result = useCaseToJson.parse(
            """
            ### UseCase: usecase1 (1)  
            #### Description
            The description 
            #### Solution
            Primary Solution
            #### Examples
            - Example 1
            ----
            ### UseCase: usecase2 (1) <foo>
            #### Description
            The description
            #### Solution
            Primary Solution
            #### Examples
            - Example 1
            ----""",
        )
        assertEquals(1, result.errors!!.size)
        assertEquals("The same examples were used in multiple Use Cases: [usecase1, usecase2] Examples: [- Example 1]", result.errors[0].message)
    }

    @Test
    fun `parse example errors - no error when conditionals are different`() {
        val result = useCaseToJson.parse(
            """
            ### UseCase: usecase1 (1) <bar>
            #### Description
            The description 
            #### Solution
            Primary Solution
            #### Examples
            - Example 1
            ----
            ### UseCase: usecase2 (1) <foo>
            #### Description
            The descriptions
            #### Solution
            Primary Solution
            #### Examples
            - Example 1
            ----""",
        )
        assertEquals(0, result.errors!!.size)
    }

    @Test
    fun `parse example errors - error when conditionals match`() {
        val result = useCaseToJson.parse(
            """
            ### UseCase: usecase1 (1) <bar>
            #### Description
            The description 
            #### Solution
            Primary Solution
            #### Examples
            - Example 1
            ----
            ### UseCase: usecase2 (1) <foo> <bar>
            #### Description
            The descriptions
            #### Solution
            Primary Solution
            #### Examples
            - Example 1
            ----""",
        )
        assertEquals(1, result.errors!!.size)
        assertEquals("The same examples were used in multiple Use Cases: [usecase1, usecase2] Examples: [- Example 1]", result.errors[0].message)
    }

    @Test
    fun `parse id errors`() {
        val result = useCaseToJson.parse(
            """
            ### UseCase: usecase1 (1)  
            #### Description
            The description 
            #### Solution
            Primary Solution
            ----
            ### UseCase: usecase1 (1) 
            #### Description
            The description
            #### Solution
            Primary Solution
            ----""",
        )
        assertEquals(1, result.errors!!.size)
        assertEquals("Duplicate use case IDs found: usecase1", result.errors[0].message)
    }

    @Test
    fun `parse id errors - error with conditionals`() {
        val result = useCaseToJson.parse(
            """
            ### UseCase: usecase1 (1)  
            #### Description
            The description 
            #### Solution
            Primary Solution
            ----
            ### UseCase: usecase1 (1) <foo>
            #### Description
            The description
            #### Solution
            Primary Solution
            ----""",
        )
        assertEquals(1, result.errors!!.size)
        assertEquals("Duplicate use case IDs found: usecase1", result.errors[0].message)
    }

    @Test
    fun `parse id errors - no error when conditionals are different`() {
        val result = useCaseToJson.parse(
            """
            ### UseCase: usecase1 (1) <bar>
            #### Description
            The description 
            #### Solution
            Primary Solution
            ----
            ### UseCase: usecase1 (1) <foo>
            #### Description
            The descriptions
            #### Solution
            Primary Solution
            ----""",
        )
        assertEquals(0, result.errors!!.size)
    }

    @Test
    fun `parse id errors - error when conditionals match`() {
        val result = useCaseToJson.parse(
            """
            ### UseCase: usecase1 (1) <bar>
            #### Description
            The description 
            #### Solution
            Primary Solution
            ----
            ### UseCase: usecase1 (1) <foo> <bar>
            #### Description
            The descriptions
            #### Solution
            Primary Solution
            ----""",
        )
        assertEquals(1, result.errors!!.size)
        assertEquals("Duplicate use case IDs found: usecase1", result.errors[0].message)
    }
}
