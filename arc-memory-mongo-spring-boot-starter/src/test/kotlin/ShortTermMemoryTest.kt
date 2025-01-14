// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.memory.mongo

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

class ShortTermMemoryTest : TestBase() {

    @Test
    fun `store entry`(): Unit = runBlocking {
        val value = "value"
        val key = UUID.randomUUID().toString()

        memory.storeShortTerm("owner", key, value, "sessionId")
        val storedValue = memory.fetch("owner", key, "sessionId")
        assertThat(storedValue).isEqualTo(value)
    }

    @Test
    fun `store complex entry`(): Unit = runBlocking {
        val value = TestValue(23, "value")
        val key = UUID.randomUUID().toString()

        memory.storeShortTerm("owner", key, value, "sessionId")
        val storedValue = memory.fetch("owner", key, "sessionId")
        assertThat(storedValue).isEqualTo(value)
    }

    @Test
    fun `entry deleted after TTL`(): Unit = runBlocking {
        val value = TestValue(23, "value")
        val key = UUID.randomUUID().toString()

        memory.storeShortTerm("owner", key, value, "sessionId")
        delay(60_000)
        val storedValue = memory.fetch("owner", key, "sessionId")
        assertThat(storedValue).isNull()
    }

    @Test
    fun `overwrite entry`(): Unit = runBlocking {
        val valueA = "valueA"
        val valueB = "valueB"
        val key = UUID.randomUUID().toString()

        memory.storeShortTerm("owner", key, valueA, "sessionId")
        memory.storeShortTerm("owner", key, valueB, "sessionId")
        val storedValue = memory.fetch("owner", key, "sessionId")
        assertThat(storedValue).isEqualTo(valueB)
    }

    @Test
    fun `store same entry under different session id`(): Unit = runBlocking {
        val valueA = "valueA"
        val valueB = "valueB"
        val key = UUID.randomUUID().toString()

        memory.storeShortTerm("owner", key, valueA, "sessionId1")
        memory.storeShortTerm("owner", key, valueB, "sessionId2")
        val storedValue = memory.fetch("owner", key, "sessionId1")
        assertThat(storedValue).isNotNull
        assertThat(storedValue).isNotEqualTo(valueB)
    }

    @Test
    fun `store same entry under different owner id`(): Unit = runBlocking {
        val valueA = "valueA"
        val valueB = "valueB"
        val key = UUID.randomUUID().toString()

        memory.storeShortTerm("owner1", key, valueA, "sessionId")
        memory.storeShortTerm("owner2", key, valueB, "sessionId")
        val storedValue = memory.fetch("owner1", key, "sessionId")
        assertThat(storedValue).isNotNull
        assertThat(storedValue).isNotEqualTo(valueB)
    }

    @Test
    fun `store same entry under different key id`(): Unit = runBlocking {
        val valueA = "valueA"
        val valueB = "valueB"
        val key1 = UUID.randomUUID().toString()
        val key2 = UUID.randomUUID().toString()

        memory.storeShortTerm("owner", key1, valueA, "sessionId")
        memory.storeShortTerm("owner", key2, valueB, "sessionId")
        val storedValue = memory.fetch("owner", key1, "sessionId")
        assertThat(storedValue).isNotNull
        assertThat(storedValue).isNotEqualTo(valueB)
    }

    @Test
    fun `delete entry`(): Unit = runBlocking {
        val valueA = "valueA"
        val key1 = UUID.randomUUID().toString()
        val key2 = UUID.randomUUID().toString()

        memory.storeShortTerm("owner", key1, valueA, "sessionId")
        memory.storeShortTerm("owner", key2, valueA, "sessionId")

        memory.storeShortTerm("owner", key1, null, "sessionId")
        val deletedValue = memory.fetch("owner", key1, "sessionId")
        assertThat(deletedValue).isNull()

        val storedValue = memory.fetch("owner", key2, "sessionId")
        assertThat(storedValue).isNotNull()
    }
}
