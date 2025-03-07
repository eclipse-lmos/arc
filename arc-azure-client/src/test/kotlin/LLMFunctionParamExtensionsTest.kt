// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.azure

import com.azure.core.util.BinaryData
import com.azure.json.JsonOptions
import com.azure.json.implementation.DefaultJsonWriter
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions
import org.eclipse.lmos.arc.agents.functions.ParameterSchema
import org.eclipse.lmos.arc.agents.functions.ParametersSchema
import org.eclipse.lmos.arc.agents.functions.toJsonMap
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class LLMFunctionParamExtensionsTest {

    @Test
    fun `test toAzureOpenAIJson for ParametersSchema with nested objects and arrays`() {
        // Create a sample ParametersSchema object
        val parametersSchema = ParametersSchema(
            required = listOf("id", "name"),
            properties = mapOf(
                "id" to ParameterSchema(
                    description = "The ID of the object",
                    type = "integer",
                ),
                "name" to ParameterSchema(
                    description = "The name of the object",
                    type = "string",
                ),
                "category" to ParameterSchema(
                    type = "object",
                    description = "The category object",
                    properties = mapOf(
                        "id" to ParameterSchema(
                            name = "id",
                            description = "Category ID",
                            type = "integer",
                        ),
                        "name" to ParameterSchema(
                            name = "name",
                            description = "Category Name",
                            type = "string",
                        ),
                    ),
                ),
                "tags" to ParameterSchema(
                    description = "List of tags",
                    type = "array",
                    items = ParameterSchema(
                        type = "object",
                        properties = mapOf(
                            "id" to ParameterSchema(
                                name = "id",
                                description = "Tag ID",
                                type = "integer",
                            ),
                            "name" to ParameterSchema(
                                name = "name",
                                description = "Tag Name",
                                type = "string",
                            ),
                        ),
                    ),
                ),
            ),
        )

        val parameters = BinaryData.fromObject(parametersSchema.toJsonMap())
        val stream = ByteArrayOutputStream()
        val jsonWriter = DefaultJsonWriter.toStream(stream, JsonOptions())
        parameters.writeTo(jsonWriter)
        jsonWriter.flush()

        // Validate the conversion result
        Assertions.assertThat(Json.parseToJsonElement(stream.toString())).isEqualTo(
            Json.parseToJsonElement(
                """{
  "type": "object",
  "properties": {
    "id": {
      "type": "integer",
      "description": "The ID of the object"
    },
    "name": {
      "type": "string",
      "description": "The name of the object"
    },
    "category": {
      "type": "object",
      "description": "The category object",
      "properties": {
        "id": {
          "type": "integer",
          "description": "Category ID"
        },
        "name": {
          "type": "string",
          "description": "Category Name"
        }
      }
    },
    "tags": {
      "type": "array",
      "description": "List of tags",
      "items": {
        "type": "object",
        "properties": {
          "id": {
            "type": "integer",
            "description": "Tag ID"
          },
          "name": {
            "type": "string",
            "description": "Tag Name"
          }
        }
      }
    }
  },
  "required": [
    "id",
    "name"
  ]
}
        """,
            ),
        )
    }
}
