// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.assistants.support.usecases.parseConditions
import org.junit.jupiter.api.Test

class ConditionsParserTest : TestBase() {

    @Test
    fun `test single condition is parsed`(): Unit = runBlocking {
        val (text, conditions) = "This is a test <mobile>".parseConditions()
        assertThat(text).isEqualTo("This is a test")
        assertThat(conditions).containsOnly("mobile")

        val (text2, conditions2) = "<mobile> This is a test".parseConditions()
        assertThat(text2).isEqualTo("This is a test")
        assertThat(conditions2).containsOnly("mobile")
    }

    @Test
    fun `test multiple conditions are parsed`(): Unit = runBlocking {
        val (text, conditions) = "This is a test <mobile, web>".parseConditions()
        assertThat(text).isEqualTo("This is a test")
        assertThat(conditions).containsOnly("mobile", "web")

        val (text2, conditions2) = "<mobile,web> This is a test".parseConditions()
        assertThat(text2).isEqualTo("This is a test")
        assertThat(conditions2).containsOnly("mobile", "web")
    }
}
