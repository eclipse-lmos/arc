// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.assistants.support.usecases

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.eclipse.lmos.arc.assistants.support.usecases.features.mustache
import org.junit.jupiter.api.Test

class MustacheVariablesTest {

    @Test
    fun `test mustache formatter`(): Unit = runBlocking {
        val useCase = """
            ### UseCase: default
            #### Description
            Hello {{name}}!
        """.trimIndent()

        val formatted = useCase.toUseCases().formatToString(
            formatter = mustache(mapOf("name" to "World"))
        )

        Assertions.assertThat(formatted).contains("Hello World!")
    }
}