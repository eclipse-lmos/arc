
// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.azure

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.eclipse.lmos.arc.agents.functions.ParameterSchema
import org.eclipse.lmos.arc.agents.functions.ParameterType
import org.eclipse.lmos.arc.agents.functions.ParametersSchema
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LLMFunctionParamExtensionsTest {

    @Test
    fun `test toAzureOpenAIJson for ParametersSchema with nested objects and arrays`() {
        // Create a sample ParametersSchema object
        val parametersSchema = ParametersSchema(
            required = listOf("id", "name"),
            parameters = listOf(
                ParameterSchema(
                    name = "id",
                    description = "The ID of the object",
                    type = ParameterType("integer"),
                    enum = emptyList(),
                ),
                ParameterSchema(
                    name = "name",
                    description = "The name of the object",
                    type = ParameterType("string"),
                    enum = emptyList(),
                ),
                ParameterSchema(
                    name = "category",
                    description = "The category object",
                    type = ParameterType(
                        schemaType = "object",
                        properties = listOf(
                            ParameterSchema(
                                name = "id",
                                description = "Category ID",
                                type = ParameterType("integer"),
                                enum = emptyList(),
                            ),
                            ParameterSchema(
                                name = "name",
                                description = "Category Name",
                                type = ParameterType("string"),
                                enum = emptyList(),
                            ),
                        ),
                    ),
                    enum = emptyList(),
                ),
                ParameterSchema(
                    name = "tags",
                    description = "List of tags",
                    type = ParameterType(
                        schemaType = "array",
                        items = ParameterType(
                            schemaType = "object",
                            properties = listOf(
                                ParameterSchema(
                                    name = "id",
                                    description = "Tag ID",
                                    type = ParameterType("integer"),
                                    enum = emptyList(),
                                ),
                                ParameterSchema(
                                    name = "name",
                                    description = "Tag Name",
                                    type = ParameterType("string"),
                                    enum = emptyList(),
                                ),
                            ),
                        ),
                    ),
                    enum = emptyList(),
                ),
            ),
        )

        // Convert to Azure OpenAI JSON format
        val jsonResult: JsonObject = parametersSchema.toAzureOpenAIObject()

        // Expected JSON structure
        val expectedJson = buildJsonObject {
            put("type", JsonPrimitive("object"))
            put("required", JsonArray(listOf(JsonPrimitive("id"), JsonPrimitive("name"))))
            put(
                "properties",
                buildJsonObject {
                    put(
                        "id",
                        buildJsonObject {
                            put("type", JsonPrimitive("integer"))
                            put("description", JsonPrimitive("The ID of the object"))
                        },
                    )
                    put(
                        "name",
                        buildJsonObject {
                            put("type", JsonPrimitive("string"))
                            put("description", JsonPrimitive("The name of the object"))
                        },
                    )
                    put(
                        "category",
                        buildJsonObject {
                            put("type", JsonPrimitive("object"))
                            put("description", JsonPrimitive("The category object"))
                            put(
                                "properties",
                                buildJsonObject {
                                    put(
                                        "id",
                                        buildJsonObject {
                                            put("type", JsonPrimitive("integer"))
                                            put("description", JsonPrimitive("Category ID"))
                                        },
                                    )
                                    put(
                                        "name",
                                        buildJsonObject {
                                            put("type", JsonPrimitive("string"))
                                            put("description", JsonPrimitive("Category Name"))
                                        },
                                    )
                                },
                            )
                        },
                    )
                    put(
                        "tags",
                        buildJsonObject {
                            put("type", JsonPrimitive("array"))
                            put("description", JsonPrimitive("List of tags"))
                            put(
                                "items",
                                buildJsonObject {
                                    put("type", JsonPrimitive("object"))
                                    put(
                                        "properties",
                                        buildJsonObject {
                                            put(
                                                "id",
                                                buildJsonObject {
                                                    put("type", JsonPrimitive("integer"))
                                                    put("description", JsonPrimitive("Tag ID"))
                                                },
                                            )
                                            put(
                                                "name",
                                                buildJsonObject {
                                                    put("type", JsonPrimitive("string"))
                                                    put("description", JsonPrimitive("Tag Name"))
                                                },
                                            )
                                        },
                                    )
                                },
                            )
                        },
                    )
                },
            )
        }

        // Validate the conversion result
        assertEquals(expectedJson, jsonResult, "The JSON conversion is not correct.")
    }
}
