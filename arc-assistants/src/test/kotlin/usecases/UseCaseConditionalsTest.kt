// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.assistants.support.usecases

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UseCaseConditionalsTest {

    @Test
    fun `test simple use case conditional - no match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1 <foo>
            #### Description
            The description of the use case 1.
            #### Solution 
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString().trim()
        assertThat(result).isEqualTo("")
    }

    @Test
    fun `test simple use case conditional - no match multiple conditions`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1 <foo, bar>
            #### Description
            The description of the use case 1.
            #### Solution 
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(conditions = setOf("foo")).trim()
        assertThat(result).isEqualTo("")
    }

    @Test
    fun `test simple use case conditional - match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1 <foo>
            #### Description
            The description of the use case 1.
            #### Solution 
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(conditions = setOf("foo")).trim()
        assertThat(result).isEqualTo(
            """
           |### UseCase: usecase1
           |#### Description
           |The description of the use case 1.
           |
           |#### Solution
           |Solution
           |
           |----
            """.trimMargin(),
        )
    }
}
