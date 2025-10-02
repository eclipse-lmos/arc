// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
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

        val (text3, conditions3) = "<mobile, !web>This is a test".parseConditions()
        assertThat(text3).isEqualTo("This is a test")
        assertThat(conditions3).containsOnly("mobile", "!web")

        val (text4, conditions4) = "<mobile><fixed>This is a test".parseConditions()
        assertThat(text4).isEqualTo("This is a test")
        assertThat(conditions4).containsOnly("mobile", "fixed")

        val (text5, conditions5) = "<mobile>This is a <fixed>test".parseConditions()
        assertThat(text5).isEqualTo("This is a test")
        assertThat(conditions5).containsOnly("mobile", "fixed")
    }

    @Test
    fun `test negative conditions are parsed`(): Unit = runBlocking {
        val (text, conditions) = "This is a test <mobile, !web>".parseConditions()
        assertThat(text).isEqualTo("This is a test")
        assertThat(conditions).containsOnly("mobile", "!web")
    }
}
