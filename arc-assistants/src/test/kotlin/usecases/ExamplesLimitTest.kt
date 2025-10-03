// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ExamplesLimitTest {

    @Test
    fun `test the examples with no limit`(): Unit = runBlocking {
        val useCases = """
        ### UseCase: usecase
        #### Description
        The description of the use case 2.

        #### Solution
        Primary Solution
        
        #### Examples
         - 1
         - 2
         - 3
         - 4
         - 5
        ----
            """.toUseCases()
        val parsedUseCases = useCases.formatToString().trimMargin().trim()
        assertThat(parsedUseCases).contains("#### Examples")
        assertThat(parsedUseCases).contains("- 1\n")
        assertThat(parsedUseCases).contains("- 2\n")
        assertThat(parsedUseCases).contains("- 3\n")
        assertThat(parsedUseCases).contains("- 4\n")
        assertThat(parsedUseCases).contains("- 5\n")
    }

    @Test
    fun `test the examples limit`(): Unit = runBlocking {
        val useCases = """
        ### UseCase: usecase
        #### Description
        The description of the use case 2.

        #### Solution
        Primary Solution
        
        #### Examples
         - 1
         - 2
         - 3
         - 4
         - 5
        ----
            """.toUseCases()
        val parsedUseCases = useCases.formatToString(exampleLimit = 2).trimMargin().trim()
        assertThat(parsedUseCases).contains("#### Examples")
        assertThat(parsedUseCases).contains("- 1\n")
        assertThat(parsedUseCases).contains("- 2\n")
        assertThat(parsedUseCases).doesNotContain("- 3\n")
        assertThat(parsedUseCases).doesNotContain("- 4\n")
        assertThat(parsedUseCases).doesNotContain("- 5\n")
    }
}
