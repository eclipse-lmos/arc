// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.TestBase
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.dsl.BasicDSLContext
import org.junit.jupiter.api.Test

class ExtractTest : TestBase() {

    @Test
    fun `test extract emails`(): Unit = runBlocking {
        val context = BasicDSLContext(testBeanProvider)
        val input = """
           This is my email address: pat+man@gmail.com
        and this karl.man@telekom.de, and this (my@email.com), 
        but not this email.com.
       """
        val result = context.extractEmail(UserMessage(input))
        assertThat(result).contains("pat+man@gmail.com", "karl.man@telekom.de", "my@email.com")
    }

    @Test
    fun `test extract empty email list`(): Unit = runBlocking {
        val context = BasicDSLContext(testBeanProvider)
        val input = """
           This has no emails. Not even me@me
       """
        val result = context.extractEmail(UserMessage(input))
        assertThat(result).isEmpty()
    }

    @Test
    fun `test extract urls`(): Unit = runBlocking {
        val context = BasicDSLContext(testBeanProvider)
        val input = """
           This is http://www.google.com, https://www.google.com, 
           and http://telekom.de/path?name=name
       """
        val result = context.extractUrl(UserMessage(input))
        assertThat(result).contains(
            "http://www.google.com",
            "https://www.google.com",
            "http://telekom.de/path?name=name",
        )
    }

    @Test
    fun `test extract empty urls list`(): Unit = runBlocking {
        val context = BasicDSLContext(testBeanProvider)
        val input = """
           This has no urls. Not even me@me
       """
        val result = context.extractUrl(UserMessage(input))
        assertThat(result).isEmpty()
    }
}
