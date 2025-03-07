// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.functions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ParameterSchemaTest {

    @Test
    fun `test toJsonMap`() {
        val schema = ParametersSchema(
            properties = mapOf(
                "name" to ParameterSchema(
                    type = "string",
                    description = "this is a test property",
                ),
                "array" to ParameterSchema(
                    type = "array",
                    items = ParameterSchema(type = "integer"),
                ),
                "number" to ParameterSchema(
                    type = "integer",
                ),
                "object" to ParameterSchema(
                    type = "object",
                    properties = mapOf(
                        "key" to ParameterSchema(type = "string"),
                    ),
                ),
            ),
        ).toJsonMap()
        assertThat(schema).isEqualTo(
            mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "name" to mapOf(
                        "type" to "string",
                        "description" to "this is a test property",
                    ),
                    "array" to mapOf(
                        "type" to "array",
                        "items" to mapOf("type" to "integer"),
                    ),
                    "number" to mapOf(
                        "type" to "integer",
                    ),
                    "object" to mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "key" to mapOf("type" to "string"),
                        ),
                    ),
                ),
            ),
        )
    }
}
