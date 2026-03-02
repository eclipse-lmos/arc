// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.schema

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class JsonSchemaConverterTest {

    private val converter = JsonSchemaConverter
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class SimpleData(
        val name: String,
        val age: Int,
        val active: Boolean
    )

    @Test
    fun `should generate schema for simple data class`() {
        val schemaString = converter.generate(SimpleData::class)
        val schema = json.parseToJsonElement(schemaString).jsonObject

        assertEquals("object", schema["type"]?.jsonPrimitive?.content)
        assertEquals("false", schema["additionalProperties"]?.jsonPrimitive?.content)

        val properties = schema["properties"]?.jsonObject
        assertNotNull(properties)

        assertEquals("string", properties!!["name"]?.jsonObject?.get("type")?.jsonPrimitive?.content)
        assertEquals("integer", properties["age"]?.jsonObject?.get("type")?.jsonPrimitive?.content)
        assertEquals("boolean", properties["active"]?.jsonObject?.get("type")?.jsonPrimitive?.content)

        val required = schema["required"]?.jsonArray?.map { it.jsonPrimitive.content }
        assertNotNull(required)
        assertTrue(required!!.contains("name"))
        assertTrue(required.contains("age"))
        assertTrue(required.contains("active"))
    }

    @Serializable
    data class NestedData(
        val simple: SimpleData,
        val description: String
    )

    @Test
    fun `should generate schema that handles nested objects correctly`() {
        val schemaString = converter.generate(NestedData::class)
        val schema = json.parseToJsonElement(schemaString).jsonObject

        assertEquals("false", schema["additionalProperties"]?.jsonPrimitive?.content)

        val properties = schema["properties"]?.jsonObject
        assertNotNull(properties)

        val simpleProp = properties!!["simple"]?.jsonObject
        assertEquals("object", simpleProp?.get("type")?.jsonPrimitive?.content)
        assertEquals("false", simpleProp?.get("additionalProperties")?.jsonPrimitive?.content)

        val nestedProps = simpleProp?.get("properties")?.jsonObject
        assertNotNull(nestedProps)
        assertEquals("string", nestedProps!!["name"]?.jsonObject?.get("type")?.jsonPrimitive?.content)
    }

    @Serializable
    data class ListData(
        val items: List<String>
    )

    @Test
    fun `should generate schema for list property`() {
        val schemaString = converter.generate(ListData::class)
        val schema = json.parseToJsonElement(schemaString).jsonObject

        val properties = schema["properties"]?.jsonObject
        val itemsProp = properties!!["items"]?.jsonObject

        assertEquals("array", itemsProp?.get("type")?.jsonPrimitive?.content)

        val itemType = itemsProp?.get("items")?.jsonObject
        assertEquals("string", itemType?.get("type")?.jsonPrimitive?.content)
    }

    @Serializable
    data class MapData(
        val attributes: Map<String, Int>
    )

    @Test
    fun `should generate schema for map property`() {
        val schemaString = converter.generate(MapData::class)
        val schema = json.parseToJsonElement(schemaString).jsonObject

        val properties = schema["properties"]?.jsonObject
        val attributesProp = properties!!["attributes"]?.jsonObject

        assertEquals("object", attributesProp?.get("type")?.jsonPrimitive?.content)

        val additionalProperties = attributesProp?.get("additionalProperties")?.jsonObject
        assertEquals("integer", additionalProperties?.get("type")?.jsonPrimitive?.content)
    }

    @Serializable
    enum class Status {
        ACTIVE, INACTIVE
    }

    @Serializable
    data class EnumData(
        val status: Status
    )

    @Test
    fun `should generate schema for enum property`() {
        val schemaString = converter.generate(EnumData::class)
        val schema = json.parseToJsonElement(schemaString).jsonObject

        val properties = schema["properties"]?.jsonObject
        val statusProp = properties!!["status"]?.jsonObject

        assertEquals("string", statusProp?.get("type")?.jsonPrimitive?.content)

        val enumValues = statusProp?.get("enum")?.jsonArray?.map { it.jsonPrimitive.content }
        assertNotNull(enumValues)
        assertTrue(enumValues!!.contains("ACTIVE"))
        assertTrue(enumValues.contains("INACTIVE"))
    }

    @Serializable
    data class OptionalData(
        val requiredField: String,
        val optionalField: String = "default"
    )

    @Test
    fun `should exclude optional fields from required list`() {
        val schemaString = converter.generate(OptionalData::class)
        val schema = json.parseToJsonElement(schemaString).jsonObject

        val required = schema["required"]?.jsonArray?.map { it.jsonPrimitive.content }
        assertNotNull(required)
        assertTrue(required!!.contains("requiredField"))
        assertTrue(!required.contains("optionalField"))
    }
}

