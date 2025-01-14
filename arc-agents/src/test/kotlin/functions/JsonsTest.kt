// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.functions

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.TestBase
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.getOrThrow
import org.junit.jupiter.api.Test

class JsonsTest : TestBase() {

    @Test
    fun `test convertToJsonMap`() {
        val jsonMap = """
            { 
              "name": "Bard", 
              "array": [1,2],
              "number": 3,
              "object": { "key": "value" }
             }
            """.convertToJsonMap().getOrThrow()
        assertThat(jsonMap["name"]).isEqualTo("Bard")
        assertThat(jsonMap["array"] as List<Int>).contains(1, 2)
        assertThat(jsonMap["number"] as Int).isEqualTo(3)
        assertThat(jsonMap["object"] as Map<String, Any?>).containsEntry("key", "value")
    }

    @Test
    fun `test convertToJsonMap fails`() {
        val result = """
            { 
              "name": "B
            """.convertToJsonMap()
        assertThat(result).isInstanceOf(Failure::class.java)
        assertThat((result as Failure).reason).isInstanceOf(InvalidJsonException::class.java)
    }
}
