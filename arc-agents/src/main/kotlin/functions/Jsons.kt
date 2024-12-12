// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.functions

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.eclipse.lmos.arc.core.failWith
import org.eclipse.lmos.arc.core.result

/**
 * Converts json strings to a map of any types.
 */
fun String.convertToJsonMap() = result<Map<String, Any?>, InvalidJsonException> {
    try {
        if (isEmpty()) return@result emptyMap()
        val jsonMap = Json.decodeFromString<Map<String, JsonElement>>(this@convertToJsonMap)
        jsonMap.mapValues { it.value.extractedContent }
    } catch (ex: SerializationException) {
        failWith { InvalidJsonException(this@convertToJsonMap, ex) }
    }
}

/**
 * Converts JsonElements to their primitive types.
 */
val JsonElement.extractedContent: Any?
    get() {
        if (this is JsonPrimitive) {
            if (this.jsonPrimitive.isString) {
                return this.jsonPrimitive.content
            }
            return this.jsonPrimitive.booleanOrNull ?: this.jsonPrimitive.intOrNull ?: this.jsonPrimitive.longOrNull
                ?: this.jsonPrimitive.floatOrNull ?: this.jsonPrimitive.doubleOrNull ?: this.jsonPrimitive.contentOrNull
        }
        if (this is JsonArray) {
            return this.jsonArray.map {
                it.extractedContent
            }
        }
        if (this is JsonObject) {
            return this.jsonObject.entries.associate {
                it.key to it.value.extractedContent
            }
        }
        return null
    }

/**
 * Throw when invalid json is encountered.
 */
class InvalidJsonException(invalidJson: String, ex: SerializationException) :
    Exception("Encountered invalid json: $invalidJson", ex)
