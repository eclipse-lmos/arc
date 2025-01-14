// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException

class ResultTest {

    @Test
    fun `test result returns result`() {
        val result = result<String, Exception> {
            "Test"
        }
        assertThat(result).isInstanceOf(Success::class.java)
        assertThat((result as Success).value).isEqualTo("Test")
    }

    @Test
    fun `test result block catches exceptions`() {
        val result = result<String, Exception> {
            error("Test")
        }
        assertThat(result).isInstanceOf(Failure::class.java)
        assertThat((result as Failure).reason).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `test result executes finally block`() {
        var finallyBlockExecuted = false
        result<String, Exception> {
            finally {
                finallyBlockExecuted = true
            }
            "Test"
        }
        assertThat(finallyBlockExecuted).isTrue()
    }

    @Test
    fun `test finally block is passed result`() {
        result<String, Exception> {
            finally { result ->
                assertThat(result.getOrThrow()).isEqualTo("Test")
            }
            "Test"
        }
    }

    @Test
    fun `test finally block is passed failed result`() {
        result<String, Exception> {
            finally {
                assertThat(it).isInstanceOf(Failure::class.java)
                assertThat((it as Failure).reason).isInstanceOf(IllegalStateException::class.java)
            }
            error("Test")
        }
    }

    @Test
    fun `test result block catches exceptions and executes finally block`() {
        var finallyBlockExecuted = false
        result<String, Exception> {
            finally {
                finallyBlockExecuted = true
            }
            error("Test")
        }
        assertThat(finallyBlockExecuted).isTrue()
    }

    @Test
    fun `test failWith returns exception`() {
        val result = result<String, IllegalStateException> {
            failWith { IllegalStateException("test") }
        }
        assertThat(result).isInstanceOf(Failure::class.java)
        assertThat((result as Failure).reason).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `test failWith on Failure type`() {
        val result = result<String, IllegalStateException> {
            Failure(IOException()) failWith { IllegalStateException("test") }
        }
        assertThat(result).isInstanceOf(Failure::class.java)
        assertThat((result as Failure).reason).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `test failWith on Success type`() {
        val result = result<String, IllegalStateException> {
            val s = Success("test") failWith { IllegalStateException("err") }
            s
        }
        assertThat(result).isInstanceOf(Success::class.java)
        assertThat((result as Success).value).isEqualTo("test")
    }

    @Test
    fun `test ensureNotNull`() {
        val param = null
        val result = result<String, IllegalStateException> {
            ensureNotNull(param) { IllegalStateException("test") }
            "Test"
        }
        assertThat(result).isInstanceOf(Failure::class.java)
        assertThat((result as Failure).reason).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `test ensure`() {
        val someAssertion = true
        val result = result<String, IllegalStateException> {
            ensure(someAssertion) { IllegalStateException("err") }
            "Test"
        }
        assertThat(result).isInstanceOf(Success::class.java)
        assertThat((result as Success).value).isEqualTo("Test")
    }

    @Test
    fun `test ensure fails`() {
        val someAssertion = false
        val result = result<String, IllegalStateException> {
            ensure(someAssertion) { IllegalStateException("err") }
            "Test"
        }
        assertThat(result).isInstanceOf(Failure::class.java)
        assertThat((result as Failure).reason).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `test map success`() {
        val result = Success("in").map { "out" }
        assertThat(result).isInstanceOf(Success::class.java)
        assertThat((result as Success).value).isEqualTo("out")
    }

    @Test
    fun `test map failure`() {
        val result = Failure(RuntimeException("")).map { "out" }
        assertThat(result).isInstanceOf(Failure::class.java)
        assertThat((result as Failure).reason).isInstanceOf(RuntimeException::class.java)
    }
}
