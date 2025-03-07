// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl

import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.agents.functions.toJson
import org.junit.jupiter.api.Test

class FunctionDefinitionContextTest {

    @Test
    fun `test toSchemaMap with string property`() {
        val context = BasicFunctionDefinitionContext(beans())
        with(context) {
            val parametersSchema = string(name = "test", description = "this is a test property")
            val schemaMap = parametersSchema.toJson()
            assertThat(schemaMap).isEqualTo(
                Json.parseToJsonElement(
                    """
                  { 
                    "type":"string",
                     "description":"this is a test property"
                  }
               """,
                ),
            )
        }
    }

    @Test
    fun `test toSchemaMap with array property`() {
        val context = BasicFunctionDefinitionContext(beans())
        with(context) {
            val parametersSchema =
                array(
                    "arrayParam",
                    description = "this is a test property",
                    itemType = "string",
                )
            val schemaMap = parametersSchema.toJson()
            println(schemaMap)
            assertThat(schemaMap).isEqualTo(
                Json.parseToJsonElement(
                    """
                  {
                      "type": "array",
                      "description": "this is a test property",
                      "items": {
                          "type": "string"
                      }
                }
              """,
                ),
            )
        }
    }

    @Test
    fun `test toSchemaMap with object property`() {
        val context = BasicFunctionDefinitionContext(beans())
        with(context) {
            val parametersSchema =
                objectType(
                    "objectParam",
                    description = "this is a test property",
                    properties = listOf(
                        string("city", "the city", required = true),
                        string("time", "the time of day.", required = false),
                    ),
                )
            val schemaMap = parametersSchema.toJson()
            assertThat(schemaMap).isEqualTo(
                Json.parseToJsonElement(
                    """
                 {
                   "description":"this is a test property", 
                   "properties":{
                      "city": {"description":"the city", "type":"string"},
                      "time": {"description":"the time of day.", "type":"string"}
                    }, 
                   "required":["city"], 
                   "type":"object"
                }
            """,
                ),
            )
        }
    }

    @Test
    fun `test toSchemaMap with types function`() {
        val context = BasicFunctionDefinitionContext(beans())
        with(context) {
            val parametersSchema =
                types(
                    string("city", "the city", required = true),
                    string("time", "the time of day.", required = false),
                )
            val schemaMap = parametersSchema.toJson()
            assertThat(schemaMap).isEqualTo(
                Json.parseToJsonElement(
                    """
                 {
                   "properties":{
                      "city": {"description":"the city", "type":"string"},
                      "time": {"description":"the time of day.", "type":"string"}
                    }, 
                   "required":["city"], 
                   "type":"object"
                }
            """,
                ),
            )
        }
    }
}
