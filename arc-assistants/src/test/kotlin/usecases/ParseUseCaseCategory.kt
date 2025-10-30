// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.assistants.support.usecases

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ParseUseCaseCategory {

    @Test
    fun `test parsing use case with no category`() {
        val useCase = """
            ### UseCase: usecase1
            #### Description
            The description of the use case 1.
            #### Solution 
            Solution
            ----
        """.trimIndent().toUseCases().first()
        assertThat(useCase.category).isNull()
    }

    @Test
    fun `test parsing use case with category`() {
        val useCase = """
            ### UseCase: usecase1
            #### Category: category1
            #### Description
            The description of the use case 1.
            #### Solution 
            Solution
            ----
        """.trimIndent().toUseCases().first()
        assertThat(useCase.category).isEqualTo("category1")
    }

    @Test
    fun `test parsing use case with category containing spaces and special chars`() {
        val useCase = """
        ### UseCase: usecase1
        #### Category: Kategorie 1 - Test!
        #### Description
        Desc
        ----
        """.trimIndent().toUseCases().first()
        assertThat(useCase.category).isEqualTo("Kategorie 1 - Test!")
    }

    @Test
    fun `test parsing use case with empty category`() {
        try {
            """
            ### UseCase: usecase1
            #### Category:
            #### Description
            Desc
            ----
            """.trimIndent().toUseCases().first()
            Assertions.fail("Expected IllegalStateException!")
        } catch (e: IllegalStateException) {
            assertThat(e.message).isEqualTo("Missing category in: #### Category:")
        }
    }

    @Test
    fun `test parsing use case with broken category`() {
        try {
            """
            ### UseCase: usecase1
            #### Category
            #### Description
            Desc
            ----
            """.trimIndent().toUseCases()
            Assertions.fail("Expected IllegalStateException!")
        } catch (e: IllegalStateException) {
            assertThat(e.message).isEqualTo("Unknown UseCase section: #### Category")
        }
    }
}
