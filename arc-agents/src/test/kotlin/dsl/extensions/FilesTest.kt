// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.TestBase
import org.eclipse.lmos.arc.agents.dsl.BasicDSLContext
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class FilesTest : TestBase() {

    @Test
    fun `test read file from classpath`(): Unit = runBlocking {
        val context = BasicDSLContext(testBeanProvider)
        val result = context.local("test.txt")
        assertThat(result).contains("ARC is fun!")
    }

    @Test
    fun `test return null when file missing`(): Unit = runBlocking {
        val context = BasicDSLContext(testBeanProvider)
        val result = context.local("test_t.txt")
        assertThat(result).isNull()
    }

    @Test
    fun `test cache`(): Unit = runBlocking {
        val context = BasicDSLContext(testBeanProvider)
        val temp = File("test-cache.txt").also { it.writeText("ARC is very fun!") }

        var result = context.local(temp.name, 2.seconds)
        temp.delete()
        assertThat(result).contains("ARC is very fun!")

        result = context.local(temp.name)
        assertThat(result).contains("ARC is very fun!")

        delay(2500.milliseconds)
        result = context.local(temp.name)
        assertThat(result).isNull()
    }
}
