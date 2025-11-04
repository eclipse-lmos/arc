// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.assistants.support.usecases

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class ElseConditionalsTest {

    @Test
    fun `test else conditional in solution - match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo>Bar
            <else>else
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi").trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |else
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test else conditional with end tag in solution - match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo>
               Bar
            </>
            <else>
              else
            </>  
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi").trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |
           |else
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test else conditional in solution - no match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <foo>Bar
            <else>else
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("foo")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |Bar
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test else conditional in solution with multiple conditions - no match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <bar>Bar
            <foo>foo
            <else>else
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi", conditions = setOf("foo")).trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |foo
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }

    @Test
    fun `test else conditional in solution with negative conditions - no match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            <bar>Bar
            <!foo>foo
            <else>else
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "Hi").trim()
        Assertions.assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |foo
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }
}
