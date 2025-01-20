// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UseCaseIdExtractorTest {

    @Test
    fun `test use case id extraction`(): Unit = runBlocking {
        val message = """<ID:use_case01>
                 This is a reply from the LLM."""
        val (filteredMessage, useCaseId) = extractUseCaseId(message)
        assertThat(filteredMessage).contains("This is a reply from the LLM.")
        assertThat(useCaseId).contains("use_case01")
    }

    @Test
    fun `test use case null extraction`(): Unit = runBlocking {
        val message = """This is a reply from the LLM."""
        val (filteredMessage, useCaseId) = extractUseCaseId(message)
        assertThat(filteredMessage).contains("This is a reply from the LLM.")
        assertThat(useCaseId).isNull()
    }

    @Test
    fun `test step id extraction`(): Unit = runBlocking {
        val message = """<ID:use_case01> <Step 1> 
                 This is a reply from the LLM."""
        val (filteredMessage, stepId) = extractUseCaseStepId(message)
        assertThat(filteredMessage).contains(
            """<ID:use_case01>  
                 This is a reply from the LLM.""",
        )
        assertThat(stepId).contains("1")
    }

    @Test
    fun `test No Step id extraction`(): Unit = runBlocking {
        val message = """<ID:use_case01> <No Step> 
                 This is a reply from the LLM."""
        val (filteredMessage, stepId) = extractUseCaseStepId(message)
        assertThat(filteredMessage).contains(
            """<ID:use_case01>  
                 This is a reply from the LLM.""",
        )
        assertThat(stepId).isNull()
    }
}
