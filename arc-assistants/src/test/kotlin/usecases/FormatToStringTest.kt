// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FormatToStringTest {

    @Test
    fun `test format simple use case`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString().trim()
        assertThat(result).isEqualTo(
            """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.

            #### Solution
            Solution

            ----
            """.trimIndent(),
        )
    }

    @Test
    fun `test format use case with conditional - no match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1 <beta>
            #### Description
            The description of the use case 1.
            #### Solution 
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString()
        assertThat(result).isEqualTo("")
    }

    @Test
    fun `test format use case with conditional - match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1 <beta>
            #### Description
            The description of the use case 1.
            #### Solution 
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(conditions = setOf("beta")).trim()
        assertThat(result).isEqualTo(
            """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.

            #### Solution
            Solution

            ----
            """.trimIndent(),
        )
    }

    @Test
    fun `test format use case with regex conditional - no match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1 <regex:.*uc1.*>
            #### Description
            The description of the use case 1.
            #### Solution 
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString()
        assertThat(result).isEqualTo("")
    }

    @Test
    fun `test format use case with regex conditional - match`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1 <regex:.*uc1.*>
            #### Description
            The description of the use case 1.
            #### Solution 
            Solution
            ----
        """.trimIndent().toUseCases()
        val result = input.formatToString(input = "This is uc1 test").trim()
        assertThat(result).isEqualTo(
            """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.

            #### Solution
            Solution

            ----
            """.trimIndent(),
        )
    }

    @Test
    fun `test format selects duplicate use case with most matching conditions`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1 <beta>
            #### Description
            less specific
            #### Solution
            basic solution
            ----
            ### UseCase: usecase1 <beta, premium>
            #### Description
            more specific
            #### Solution
            premium solution
            ----
        """.trimIndent().toUseCases()

        val result = input.formatToString(conditions = setOf("beta", "premium")).trim()

        assertThat(result).contains("more specific")
        assertThat(result).contains("premium solution")
        assertThat(result).doesNotContain("less specific")
        assertThat(result).doesNotContain("basic solution")
    }

    @Test
    fun `test format selects first duplicate use case when matching-condition score ties`(): Unit = runBlocking {
        val input = """
            ### UseCase: usecase1 <beta>
            #### Description
            first definition
            #### Solution
            first solution
            ----
            ### UseCase: usecase1 <beta>
            #### Description
            second definition
            #### Solution
            second solution
            ----
        """.trimIndent().toUseCases()

        val result = input.formatToString(conditions = setOf("beta")).trim()

        assertThat(result).contains("first definition")
        assertThat(result).contains("first solution")
        assertThat(result).doesNotContain("second definition")
        assertThat(result).doesNotContain("second solution")
    }
}
