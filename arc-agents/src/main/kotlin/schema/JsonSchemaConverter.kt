// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.schema

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

object JsonSchemaConverter {

    private val json = Json { prettyPrint = true }

    @OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
    fun <T : Any> generate(kClass: KClass<T>): String {
        val descriptor = try {
            kClass.serializer().descriptor
        } catch (e: SerializationException) {
            throw IllegalArgumentException("Class ${kClass.simpleName} is not serializable. Please annotate it with @Serializable.", e)
        }

        val schema = convert(descriptor)
        return json.encodeToString(JsonObject.serializer(), schema)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun convert(descriptor: SerialDescriptor): JsonObject {
        return buildJsonObject {
            when (descriptor.kind) {
                PrimitiveKind.STRING -> put("type", "string")
                PrimitiveKind.BOOLEAN -> put("type", "boolean")
                PrimitiveKind.BYTE, PrimitiveKind.SHORT, PrimitiveKind.INT, PrimitiveKind.LONG -> put("type", "integer")
                PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> put("type", "number")
                StructureKind.LIST -> {
                    put("type", "array")
                    put("items", convert(descriptor.getElementDescriptor(0)))
                }
                StructureKind.MAP -> {
                    put("type", "object")
                    val keyDescriptor = descriptor.getElementDescriptor(0)
                    val valueDescriptor = descriptor.getElementDescriptor(1)

                    if (keyDescriptor.kind == PrimitiveKind.STRING) {
                         put("additionalProperties", convert(valueDescriptor))
                    } else {
                         // JSON object keys must be strings.
                         // If the key is not string, we treat it as object with arbitrary properties for now
                         put("additionalProperties", convert(valueDescriptor))
                    }
                }
                StructureKind.CLASS -> {
                    put("type", "object")
                    put("additionalProperties", false)
                    val properties = buildJsonObject {
                        for (i in 0 until descriptor.elementsCount) {
                            val name = descriptor.getElementName(i)
                            val elementDescriptor = descriptor.getElementDescriptor(i)
                            put(name, convert(elementDescriptor))
                        }
                    }
                    put("properties", properties)

                    val required = buildJsonArray {
                        for (i in 0 until descriptor.elementsCount) {
                            if (!descriptor.isElementOptional(i)) {
                                add(descriptor.getElementName(i))
                            }
                        }
                    }
                    if (required.isNotEmpty()) {
                        put("required", required)
                    }
                }
                 StructureKind.OBJECT -> {
                     put("type", "object")
                }
                SerialKind.ENUM -> {
                    put("type", "string")
                    val enumValues = buildJsonArray {
                        for (i in 0 until descriptor.elementsCount) {
                            add(descriptor.getElementName(i))
                        }
                    }
                    put("enum", enumValues)
                }
                 PolymorphicKind.SEALED -> {
                     // Basic handling for sealed classes could be added here
                     put("type", "object")
                }
                else -> {
                     put("type", "string")
                }
            }
        }
    }
}
