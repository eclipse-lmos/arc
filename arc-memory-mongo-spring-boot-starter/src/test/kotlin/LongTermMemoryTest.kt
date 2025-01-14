// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.memory.mongo

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

class LongTermMemoryTest : TestBase() {

    @Test
    fun `store entry`(): Unit = runBlocking {
        val value = "value"
        val key = UUID.randomUUID().toString()

        memory.storeLongTerm("owner", key, value)
        val storedValue = memory.fetch("owner", key)
        assertThat(storedValue).isEqualTo(value)
    }

    @Test
    fun `store complex entry`(): Unit = runBlocking {
        val value = TestValue(23, "value")
        val key = UUID.randomUUID().toString()

        memory.storeLongTerm("owner", key, value)
        val storedValue = memory.fetch("owner", key)
        assertThat(storedValue).isEqualTo(value)
    }

    @Test
    fun `overwrite entry`(): Unit = runBlocking {
        val valueA = "valueA"
        val valueB = "valueB"
        val key = UUID.randomUUID().toString()

        memory.storeLongTerm("owner", key, valueA)
        memory.storeLongTerm("owner", key, valueB)
        val storedValue = memory.fetch("owner", key)
        assertThat(storedValue).isEqualTo(valueB)
    }

    @Test
    fun `store same entry under different owner id`(): Unit = runBlocking {
        val valueA = "valueA"
        val valueB = "valueB"
        val key = UUID.randomUUID().toString()

        memory.storeLongTerm("1", key, valueA)
        memory.storeLongTerm("2", key, valueB)
        val storedValue = memory.fetch("1", key)
        assertThat(storedValue).isNotNull
        assertThat(storedValue).isNotEqualTo(valueB)
    }

    @Test
    fun `store same entry under different key id`(): Unit = runBlocking {
        val valueA = "valueA"
        val valueB = "valueB"
        val key1 = UUID.randomUUID().toString()
        val key2 = UUID.randomUUID().toString()

        memory.storeLongTerm("owner", key1, valueA)
        memory.storeLongTerm("owner", key2, valueB)
        val storedValue = memory.fetch("owner", key1)
        assertThat(storedValue).isNotNull
        assertThat(storedValue).isNotEqualTo(valueB)
    }

    @Test
    fun `delete entry`(): Unit = runBlocking {
        val valueA = "valueA"
        val key1 = UUID.randomUUID().toString()
        val key2 = UUID.randomUUID().toString()

        memory.storeLongTerm("owner", key1, valueA)
        memory.storeLongTerm("owner", key2, valueA)
        memory.storeLongTerm("owner", key1, null)
        val deletedValue = memory.fetch("owner", key1)
        assertThat(deletedValue).isNull()

        val storedValue = memory.fetch("owner", key2)
        assertThat(storedValue).isNotNull()
    }
}
